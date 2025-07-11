package com.loohp.hkbuseta.utils

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getLineColor
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranch
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable


@Immutable
data class RouteStopETASelectedRouteStop(
    val stopId: String,
    val stopIndex: Int,
    val co: Operator,
    val stopRoute: Route,
    val stopBranches: Set<Route>,
    val allBranches: List<Route>,
    val context: AppContext,
    val etaQueryOptions: Registry.EtaQueryOptions? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteStopETASelectedRouteStop) return false

        if (stopIndex != other.stopIndex) return false
        if (stopId != other.stopId) return false
        if (co != other.co) return false
        if (stopBranches != other.stopBranches) return false
        if (allBranches != other.allBranches) return false
        if (etaQueryOptions != other.etaQueryOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stopIndex
        result = 31 * result + stopId.hashCode()
        result = 31 * result + co.hashCode()
        result = 31 * result + stopBranches.hashCode()
        result = 31 * result + allBranches.hashCode()
        result = 31 * result + (etaQueryOptions?.hashCode() ?: 0)
        return result
    }
}

@Immutable
@Serializable
data class RouteStopETAData(
    val routeNumber: String,
    val hasEta: Boolean,
    val eta: List<String>,
    val minimal: String,
    val remark: String,
    val destination: String,
    val stop: String,
    val color: Long,
    val url: String
)

object RouteStopETALiveActivity {

    private var currentSelectedRouteStop: RouteStopETASelectedRouteStop? = null
    private var dataUpdateHandler: ((RouteStopETAData?) -> Unit)? = null
    private var platformName: BilingualText? = null
    private val lock = Lock()
    private var lastTrigger = 0L

    suspend fun trigger() {
        val now = currentTimeMillis()
        if (now - lastTrigger > Shared.ETA_UPDATE_INTERVAL) {
            tick()
            lastTrigger = now
        }
    }

    private fun run() {
        CoroutineScope(Dispatchers.Main).launch {
            tick()
        }
    }

    private suspend fun tick() {
        val handler = dataUpdateHandler?: return
        val selected = currentSelectedRouteStop
        if (selected == null) {
            handler.invoke(null)
        } else {
            val now = currentLocalDateTime()
            val registry = Registry.getInstanceNoUpdateCheck(selected.context)
            val branchByActiveness = selected.allBranches
                .currentBranchStatus(now, selected.context, false)
                .asSequence()
                .sortedByDescending { it.value.activeness }
                .map { it.key }
                .toList()
            val route = branchByActiveness.firstOrNull { selected.stopBranches.contains(it) }?: selected.stopRoute
            val query = registry.buildEtaQuery(
                stopId = selected.stopId,
                stopIndex = selected.stopIndex,
                co = selected.co,
                route = route,
                context = selected.context,
                options = selected.etaQueryOptions
            )
            val eta = query.query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            val stopName = selected.stopId.asStop(selected.context)!!.name[Shared.language]
            val data = RouteStopETAData(
                routeNumber = selected.co.getDisplayRouteNumber(route.routeNumber, shortened = true),
                hasEta = eta.nextScheduledBus in 0..59,
                eta = eta.buildETAText(selected.context),
                minimal = if (eta.nextScheduledBus <= 0L) "-" else eta.nextScheduledBus.toString(),
                remark = if (eta.nextScheduledBus < 0) eta.firstLine.text.string else "",
                destination = route.resolvedDestWithBranch(
                    prependTo = true,
                    branch = route,
                    selectedStop = selected.stopIndex,
                    selectedStopId = selected.stopId,
                    context = selected.context
                )[Shared.language],
                stop = if (selected.co.isTrain) stopName else "${selected.stopIndex}. $stopName",
                color = eta.nextCo.getLineColor(route.routeNumber, 0xFF000000),
                url = route.getDeepLink(selected.context, selected.stopId, selected.stopIndex)
            )
            handler.invoke(data)
        }
    }

    fun setDataUpdateHandler(name: BilingualText, handler: ((RouteStopETAData?) -> Unit)?) {
        platformName = name
        dataUpdateHandler = handler
    }

    fun isSupported(): Boolean {
        return dataUpdateHandler != null
    }

    fun getPlatformName(): BilingualText? {
        return platformName
    }

    fun isCurrentSelectedStop(selected: RouteStopETASelectedRouteStop): Boolean {
        return currentSelectedRouteStop == selected
    }

    fun setCurrentSelectedStop(selected: RouteStopETASelectedRouteStop) {
        lock.withLock {
            if (currentSelectedRouteStop != null) {
                clearCurrentSelectedStop()
            }
            currentSelectedRouteStop = selected
            run()
        }
    }

    fun clearCurrentSelectedStop() {
        lock.withLock {
            currentSelectedRouteStop = null
            run()
        }
    }

}

private fun Registry.ETAQueryResult.buildETAText(context: AppContext): List<String> {
    return when (Shared.etaDisplayMode) {
        ETADisplayMode.COUNTDOWN -> {
            val (text1, text2) = this.firstLine.shortText
            buildList {
                add("$text1 $text2")
                (2..3).forEach {
                    val (eText1, eText2) = this@buildETAText[it].shortText
                    if (eText1.isNotBlank() && eText2 == text2) {
                        add("$eText1 $eText2")
                    }
                }
            }
        }
        ETADisplayMode.CLOCK_TIME -> {
            val text1 = this.getResolvedText(1, Shared.etaDisplayMode, context).resolvedClockTime.string.trim()
            buildList {
                add(text1)
                (2..3).forEach {
                    val eText1 = this@buildETAText.getResolvedText(it, Shared.etaDisplayMode, context).resolvedClockTime.string.trim()
                    if (eText1.length > 1) {
                        add(eText1)
                    }
                }
            }
        }
        ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> {
            val text1 = this.getResolvedText(1, Shared.etaDisplayMode, context).resolvedClockTime.string.trim()
            val (text2, text3) = this.firstLine.shortText
            buildList {
                add("$text1  $text2$text3")
                (2..3).forEach {
                    val eText1 = this@buildETAText.getResolvedText(it, Shared.etaDisplayMode, context).resolvedClockTime.string.trim()
                    if (eText1.length > 1) {
                        val (eText2, eText3) = this@buildETAText[it].shortText
                        add("$eText1  $eText2$eText3")
                    }
                }
            }
        }
    }
}