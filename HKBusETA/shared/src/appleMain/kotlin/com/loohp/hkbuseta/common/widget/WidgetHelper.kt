package com.loohp.hkbuseta.common.widget

import co.touchlab.stately.collections.ConcurrentMutableList
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.FavouriteResolvedStop
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.JsonWidgetPrecomputedData
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.WidgetPrecomputedData
import com.loohp.hkbuseta.common.objects.WidgetStopData
import com.loohp.hkbuseta.common.objects.bracketsRemovalRegex
import com.loohp.hkbuseta.common.objects.busTerminusEnRegex
import com.loohp.hkbuseta.common.objects.busTerminusZhRegex
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isCircular
import com.loohp.hkbuseta.common.objects.prependTo
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranch
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.eitherContains
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.common.utils.nextLocalDateTimeAfter
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.common.utils.toggleCache
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toNSDateComponents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.currentLocale
import platform.Foundation.isiOSAppOnMac
import platform.darwin.NSObject
import kotlin.math.absoluteValue
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi


@OptIn(NativeRuntimeApi::class)
fun restrictHeapSize(bytes: Long) {
    GC.autotune = false
    GC.targetHeapBytes = bytes
    toggleCache(false)
    GC.collect()
}

data class WidgetLineEntry(
    val date: NSDateComponents? = null,
    val text: String? = null
)

fun LocalDateTime.toWidgetLineEntry(): WidgetLineEntry {
    return WidgetLineEntry(date = toNSDateComponents())
}

fun String.toWidgetLineEntry(): WidgetLineEntry {
    return WidgetLineEntry(text = this)
}

data class ExtendedWidgetData(
    private val etaLines: List<WidgetLineEntry>,
    val hasServices: Boolean,
    val resolvedStopName: String,
    val resolvedDestName: String,
    val closestStopLabel: String,
    val lastUpdatedLabel: String
) {
    fun getEtaLines(size: Int): List<WidgetLineEntry> {
        return etaLines.subList(0, size.coerceAtMost(etaLines.size))
    }
}

private fun FavouriteRouteStop.resolveStopWidget(stops: List<WidgetStopData>, branches: List<Route>, originGetter: () -> Coordinates?): FavouriteResolvedStop {
    if (favouriteStopMode == FavouriteStopMode.FIXED) {
        return FavouriteResolvedStop(index, stopId, stop, route)
    }
    val origin = originGetter.invoke()?: return FavouriteResolvedStop(index, stopId, stop, route)
    return stops
        .withIndex()
        .minBy { it.value.stop.location.distance(origin) }
        .let { FavouriteResolvedStop(it.index + 1, it.value.stopId, it.value.stop, branches.getOrElse(it.value.route) { branches.first() }) }
}

private fun Route.getCircularPivotIndexWidget(stops: List<WidgetStopData>): Int {
    if (!isCircular) return -1
    val circularMiddle = dest.zh.remove("(循環線)").trim()
    return stops.asSequence()
        .mapIndexedNotNull { i, s ->
            val name = s.stop.name.zh.trim()
            val matches = when {
                circularMiddle.contains("機場") -> name.contains("客運大樓")
                else -> name.eitherContains(circularMiddle)
            }
            if (matches) ((i + 1) to name) else null
        }
        .minByOrNull { (_, n) -> n.editDistance(circularMiddle) }
        ?.first
        ?: stops.first().stop.location.let {
            stops.asSequence()
                .mapIndexed { i, s -> (i + 1) to s }
                .maxBy { (_, s) -> s.stop.location.distance(it) }
                .first
        }
}

private fun Route.resolvedDestWithBranchWidget(prependTo: Boolean, branch: Route, selectedStop: Int, selectedStopId: String, stops: List<WidgetStopData>): BilingualText {
    val co = co.firstCo()!!
    return if ((co !== Operator.KMB && co !== Operator.GMB) || !branch.isCircular) {
        resolvedDestWithBranch(prependTo, branch)
    } else {
        val middleIndex = branch.getCircularPivotIndexWidget(stops)
        if (middleIndex >= 0) {
            val stopIndex = stops.indexesOf { it.stopId == selectedStopId }.minByOrNull { (it - selectedStop).absoluteValue }
            if (stopIndex != null && stopIndex + 1 >= middleIndex) {
                var dest = stops.last().stop.name
                if (co === Operator.GMB) {
                    dest = dest.zh.remove(bracketsRemovalRegex) withEn dest.en.replace(bracketsRemovalRegex, " ")
                }
                dest = dest.zh.remove(busTerminusZhRegex) withEn dest.en.remove(busTerminusEnRegex)
                dest.let { if (prependTo) it.prependTo() else it } + ("(循環線)" withEn "(Circular)")
            } else {
                resolvedDestWithBranch(prependTo, branch)
            }
        } else {
            resolvedDestWithBranch(prependTo, branch)
        }
    }
}

fun LocalDateTime.formatTimeWidget(): String {
    return toNSDateComponents().formatTimeWidget()
}

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
fun NSDateComponents.formatTimeWidget(): String {
    return NSCalendar.currentCalendar.dateFromComponents(this)?.let {
        NSDateFormatter().apply {
            locale = NSLocale.currentLocale
            dateStyle = NSDateFormatterNoStyle.convert()
            timeStyle = NSDateFormatterShortStyle.convert()
        }.stringFromDate(it)
    }?: "${hour.convert<Int>().pad(2)}:${minute.convert<Int>().pad(2)}"
}

expect fun CLLocationManager.authorizedForWidgetUpdates(): Boolean

private fun getGPSLocationAppleWidget(priority: LocationPriority = LocationPriority.ACCURATE): Deferred<Coordinates?> {
    val defer = CompletableDeferred<Coordinates?>()
    val objectPreferenceStore: MutableList<Any> = ConcurrentMutableList()
    when (CLLocationManager.authorizationStatus()) {
        kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
            val locationManager = CLLocationManager()

            if (locationManager.authorizedForWidgetUpdates()) {
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                        val location = didUpdateLocations.lastOrNull() as? CLLocation
                        if (location != null) {
                            defer.complete(location.toCoordinates())
                        } else {
                            defer.complete(null)
                        }
                        locationManager.stopUpdatingLocation()
                        objectPreferenceStore.remove(locationManager)
                        objectPreferenceStore.remove(this)
                    }

                    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                        defer.complete(null)
                        objectPreferenceStore.remove(locationManager)
                        objectPreferenceStore.remove(this)
                    }
                }

                locationManager.delegate = delegate
                locationManager.desiredAccuracy = priority.toCLLocationAccuracy()
                locationManager.requestLocation()
                objectPreferenceStore.add(locationManager)
                objectPreferenceStore.add(delegate)
            } else {
                defer.complete(null)
            }
        }
        else -> defer.complete(null)
    }
    return defer
}

@OptIn(ExperimentalForeignApi::class)
fun CLLocation.toCoordinates(): Coordinates {
    return coordinate.useContents { Coordinates(latitude, longitude) }
}

fun LocationPriority.toCLLocationAccuracy(): CLLocationAccuracy {
    return if (NSProcessInfo.processInfo.isiOSAppOnMac()) {
        when (this) {
            LocationPriority.MOST_ACCURATE -> kCLLocationAccuracyBestForNavigation
            else -> kCLLocationAccuracyBest
        }
    } else {
        when (this) {
            LocationPriority.FASTEST -> kCLLocationAccuracyHundredMeters
            LocationPriority.FASTER -> kCLLocationAccuracyNearestTenMeters
            LocationPriority.ACCURATE -> kCLLocationAccuracyBest
            LocationPriority.MOST_ACCURATE -> kCLLocationAccuracyBestForNavigation
        }
    }
}

@OptIn(NativeRuntimeApi::class)
fun buildPreviewWidgetData(destName: String): ExtendedWidgetData {
    try {
        val now = currentLocalDateTime()
        return ExtendedWidgetData(
            etaLines = listOf(
                LocalTime(9, 0).nextLocalDateTimeAfter(now).toWidgetLineEntry(),
                LocalTime(9, 15).nextLocalDateTimeAfter(now).toWidgetLineEntry(),
                LocalTime(9, 30).nextLocalDateTimeAfter(now).toWidgetLineEntry()
            ),
            hasServices = false,
            resolvedStopName = "",
            resolvedDestName = destName,
            closestStopLabel = "",
            lastUpdatedLabel = ""
        )
    } finally {
        GC.collect()
    }
}

@OptIn(NativeRuntimeApi::class)
fun buildWidgetData(precomputedData: String): ExtendedWidgetData {
    try {
        val widgetPrecomputedData = JsonWidgetPrecomputedData.decodeFromString<WidgetPrecomputedData>(precomputedData)
        GC.collect()
        val language = widgetPrecomputedData.language
        val resolved = widgetPrecomputedData.fav.resolveStopWidget(widgetPrecomputedData.allStops, widgetPrecomputedData.allBranches) {
            runBlocking { getGPSLocationAppleWidget().await() }
        }
        val route = resolved.route
        val resolvedStopName = if (widgetPrecomputedData.co.isBus) {
            "${resolved.index}. ${resolved.stop.name[language]}"
        } else {
            resolved.stop.name[language]
        }
        val currentBranch = widgetPrecomputedData.allBranches
            .currentBranchStatus(currentLocalDateTime(), widgetPrecomputedData.serviceDayMap, widgetPrecomputedData.holidays) { null }
            .asSequence()
            .sortedByDescending { it.value.activeness }
            .first()
            .key
        GC.collect()
        val resolvedDestName = route.resolvedDestWithBranchWidget(false, currentBranch, resolved.index, resolved.stopId, widgetPrecomputedData.allStops)[language]
        GC.collect()
        val widgetResults = getEtaWidget(resolved.stopId, resolved.index, widgetPrecomputedData.co, route, widgetPrecomputedData)
        val etaLines = widgetResults.lines.takeIf { it.isNotEmpty() }?: listOf((if (language == "en") "No scheduled departures" else "沒有預定班次").toWidgetLineEntry())
        val hasServices = widgetResults.hasServices
        val closestStopLabel = if (language == "en") " - Closest" else " - 最近"
        val lastUpdatedLabel = (if (language == "en") "Last Updated: " else "更新時間: ") + currentLocalDateTime().formatTimeWidget()
        GC.collect()
        return ExtendedWidgetData(etaLines, hasServices, resolvedStopName, resolvedDestName, closestStopLabel, lastUpdatedLabel)
    } finally {
        GC.collect()
    }
}