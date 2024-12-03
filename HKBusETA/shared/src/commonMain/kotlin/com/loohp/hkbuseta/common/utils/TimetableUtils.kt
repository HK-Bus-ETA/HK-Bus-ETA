/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.common.utils

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isNotBlank
import com.loohp.hkbuseta.common.objects.joinBilingualText
import com.loohp.hkbuseta.common.objects.journeyTimeCircular
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import kotlinx.datetime.isoDayNumber
import kotlin.math.min
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


class RouteTimetable(
    val routeNumber: String,
    val co: Operator,
    timetable: Map<OperatingWeekdays, List<TimetableEntry>>
): Map<OperatingWeekdays, List<TimetableEntry>> by timetable

fun routeTimetableOf(routeNumber: String, co: Operator, pair: Pair<OperatingWeekdays, List<TimetableEntry>>): RouteTimetable {
    return RouteTimetable(routeNumber, co, mapOf(pair = pair))
}

fun routeTimetableOf(routeNumber: String, co: Operator, vararg pairs: Pair<OperatingWeekdays, List<TimetableEntry>>): RouteTimetable {
    return RouteTimetable(routeNumber, co, pairs.toMap())
}

val LocalTime.Companion.MIDNIGHT: LocalTime by lazy { LocalTime(0, 0) }

inline fun LocalDateTime.dayOfWeek(holidays: Collection<LocalDate>, midnight: LocalTime): DayOfWeek {
    val mightNightOffset = midnight - LocalTime.MIDNIGHT
    return (this - mightNightOffset).date.dayOfWeek(holidays)
}

inline fun LocalDate.dayOfWeek(holidays: Collection<LocalDate>): DayOfWeek {
    return if (holidays.contains(this)) DayOfWeek.SUNDAY else dayOfWeek
}

inline val DayOfWeek.isWeekend: Boolean get() = when (this) {
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> true
    else -> false
}

class OperatingWeekdays(
    private val weekdays: Set<DayOfWeek>
): Set<DayOfWeek> by weekdays, Comparable<OperatingWeekdays> {

    companion object {

        @Suppress("MemberVisibilityCanBePrivate")
        val SUNDAY_FIRST = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )

        val ALL = OperatingWeekdays(DayOfWeek.entries.toSet())

        fun of(
            sunday: Boolean = false,
            monday: Boolean = false,
            tuesday: Boolean = false,
            wednesday: Boolean = false,
            thursday: Boolean = false,
            friday: Boolean = false,
            saturday: Boolean = false
        ): OperatingWeekdays {
            return fromBooleans(listOf(sunday, monday, tuesday, wednesday, thursday, friday, saturday))
        }

        internal fun fromBinaryStrings(days: List<String>): OperatingWeekdays {
            return fromBooleans(days.map { it != "0" })
        }

        private fun fromBooleans(days: List<Boolean>): OperatingWeekdays {
            return OperatingWeekdays(SUNDAY_FIRST.filterIndexed { i, _ -> days.getOrElse(i) { false } }.toSet())
        }
    }

    fun intersects(other: OperatingWeekdays): Boolean = any { other.contains(it) }

    operator fun times(other: OperatingWeekdays): OperatingWeekdays = OperatingWeekdays(DayOfWeek.entries.filter { contains(it) && other.contains(it) }.toSet())
    operator fun plus(other: OperatingWeekdays): OperatingWeekdays = OperatingWeekdays(toMutableSet().apply { addAll(other) })
    operator fun minus(other: OperatingWeekdays): OperatingWeekdays = OperatingWeekdays(toMutableSet().apply { removeAll(other) })

    val displayText: BilingualText by lazy {
        val ranges = mutableListOf<IntRange>()
        var rangeStart: Int? = null
        for (weekday in DayOfWeek.entries) {
            val index = weekday.isoDayNumber
            if (weekdays.contains(weekday)) {
                if (rangeStart == null) {
                    rangeStart = index
                }
                if (index == 7) {
                    ranges.add(rangeStart..index)
                }
            } else {
                rangeStart?.let {
                    ranges.add(it until index)
                    rangeStart = null
                }
            }
        }
        if (ranges.isEmpty() || ranges.first() == 1..7) {
            return@lazy "每天" withEn "Daily"
        }
        val weekdayNames = mutableListOf<BilingualText>()
        for (range in ranges) {
            val first = range.first
            val last = range.last
            when {
                first == last -> {
                    weekdayNames.add(first.toChineseWeekdayNumber(true) withEn first.toEnglishWeekdayNames(true))
                }
                first + 1 == last -> {
                    weekdayNames.add("${first.toChineseWeekdayNumber(true)}, ${last.toChineseWeekdayNumber(true)}" withEn "${first.toEnglishWeekdayNames(true)}, ${last.toEnglishWeekdayNames(true)}")
                }
                range == 0..6 -> {
                    return@lazy "每天" withEn "Daily"
                }
                else -> {
                    weekdayNames.add("${first.toChineseWeekdayNumber(true)}至${last.toChineseWeekdayNumber(true)}" withEn "${first.toEnglishWeekdayNames(true)} to ${last.toEnglishWeekdayNames(true)}")
                }
            }
        }
        return@lazy weekdayNames.joinBilingualText(", ".asBilingualText(), prefix = "星期" withEn "")
    }

    private val compareNumber: Int = DayOfWeek.entries.firstOrNull { weekdays.contains(it) }?.isoDayNumber?: Int.MAX_VALUE

    override fun compareTo(other: OperatingWeekdays): Int {
        return compareNumber.compareTo(other.compareNumber)
    }

}

@Immutable
sealed class TimetableEntry(
    val route: Route,
    val specialRouteRemark: BilingualText?,
    internal val compareTime: LocalTime?
) {
    abstract val hasScheduledServices: Boolean
    abstract fun toString(language: String): String
    abstract fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean
    abstract infix fun overlap(other: TimetableEntry): Boolean
    abstract fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int
    abstract fun numberOfServicesWithin(windowStart: LocalTime, windowEnd: LocalTime, firstServiceCheck: (LocalTime) -> Boolean): Int
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimetableEntry) return false
        if (route != other.route) return false
        if (specialRouteRemark != other.specialRouteRemark) return false
        if (compareTime != other.compareTime) return false
        return true
    }
    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + (specialRouteRemark?.hashCode() ?: 0)
        result = 31 * result + (compareTime?.hashCode() ?: 0)
        return result
    }
}
@Immutable
class TimetableIntervalEntry(
    route: Route,
    val start: LocalTime,
    val end: LocalTime,
    val interval: IntRange,
    specialRouteRemark: BilingualText?,
    subEntries: List<TimetableIntervalEntry>? = null
): TimetableEntry(route, specialRouteRemark, start) {
    val subEntries: List<TimetableIntervalEntry> = subEntries?: listOf(element = this)
    override val hasScheduledServices: Boolean = true
    fun mergeAfter(entry: TimetableIntervalEntry): TimetableIntervalEntry {
        return TimetableIntervalEntry(route, start, entry.end, interval merge entry.interval, specialRouteRemark, subEntries + entry.subEntries)
    }
    override fun toString(language: String): String {
        return "${start.hour.pad(2)}:${start.minute.pad(2)} - ${end.hour.pad(2)}:${end.minute.pad(2)}"
    }
    override fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean {
        return (start to end).isBetweenInclusive(windowStart, windowEnd)
    }
    override fun overlap(other: TimetableEntry): Boolean {
        if (other !is TimetableIntervalEntry) return false
        if (start == other.start || end == other.end) return true
        if (other.start.isBetweenExclusive(start, end)) return true
        if (other.end.isBetweenExclusive(start, end)) return true
        return false
    }
    override fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int {
        return if (subEntries.isNotEmpty() && subEntries.first() != this) {
            var numberOfServices = 0
            for ((index, entry) in subEntries.withIndex()) {
                numberOfServices += entry.numberOfServices { index == 0 && firstServiceCheck.invoke(it) }
            }
            numberOfServices
        } else {
            ((end - start).inWholeMinutes.toInt() / interval.middle) + if (firstServiceCheck.invoke(start)) 1 else 0
        }
    }
    override fun numberOfServicesWithin(windowStart: LocalTime, windowEnd: LocalTime, firstServiceCheck: (LocalTime) -> Boolean): Int {
        return if (subEntries.isNotEmpty() && subEntries.first() != this) {
            var numberOfServices = 0
            for ((index, entry) in subEntries.withIndex()) {
                numberOfServices += entry.numberOfServicesWithin(windowStart, windowEnd) { index == 0 && firstServiceCheck.invoke(it) }
            }
            numberOfServices
        } else {
            return (start to end).intersects(windowStart, windowEnd)?.let { (periodStart, periodEnd) ->
                if (periodStart == periodEnd) {
                    if (firstServiceCheck.invoke(periodStart)) 1 else 0
                } else {
                    (((periodEnd - periodStart).inWholeMinutes.toInt() / interval.middle) + if (firstServiceCheck.invoke(periodStart)) 1 else 0)
                }
            }?: 0
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimetableIntervalEntry) return false
        if (!super.equals(other)) return false
        if (start != other.start) return false
        if (end != other.end) return false
        if (interval != other.interval) return false
        return true
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + interval.hashCode()
        return result
    }
}
@Immutable
class TimetableSingleEntry(
    route: Route,
    val time: LocalTime,
    specialRouteRemark: BilingualText?
): TimetableEntry(route, specialRouteRemark, time) {
    override val hasScheduledServices: Boolean = true
    override fun toString(language: String): String {
        return "${time.hour.pad(2)}:${time.minute.pad(2)}"
    }
    override fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean {
        return time.isBetweenInclusive(windowStart, windowEnd)
    }
    override infix fun overlap(other: TimetableEntry): Boolean {
        return other is TimetableSingleEntry && time == other.time
    }
    override fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int {
        return 1
    }
    override fun numberOfServicesWithin(windowStart: LocalTime, windowEnd: LocalTime, firstServiceCheck: (LocalTime) -> Boolean): Int {
        return if (within(windowStart, windowEnd)) 1 else 0
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimetableSingleEntry) return false
        if (!super.equals(other)) return false
        if (time != other.time) return false
        return true
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }
}
@Immutable
class TimetableSpecialEntry(
    route: Route,
    val notice: BilingualText,
    specialRouteRemark: BilingualText?
): TimetableEntry(route, specialRouteRemark, null) {
    override val hasScheduledServices: Boolean = false
    override fun toString(language: String): String = notice[language]
    override fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean = false
    override infix fun overlap(other: TimetableEntry): Boolean = false
    override fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int = 0
    override fun numberOfServicesWithin(windowStart: LocalTime, windowEnd: LocalTime, firstServiceCheck: (LocalTime) -> Boolean): Int = 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimetableSpecialEntry) return false
        if (!super.equals(other)) return false
        if (notice != other.notice) return false
        return true
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + notice.hashCode()
        return result
    }
}

fun List<TimetableEntry>.currentEntry(
    routeNumber: String,
    co: Operator,
    time: LocalDateTime,
    singleEntryActivePreWindowMinutes: Int = 60,
    searchPreWindow: IntRange = 0..0,
    maxResults: Int = Int.MAX_VALUE
): List<Int> {
    if (isEmpty()) return emptyList()
    if (all { it is TimetableSpecialEntry }) return listOf(element = 0)
    val singleEntryWindow = singleEntryActivePreWindowMinutes.minutes
    val compareMidnight = if (getServiceTimeCategory(routeNumber, co).day) dayServiceMidnight else nightServiceMidnight
    val compareTime = compareMidnight.atDate(time.date).let { if (it > time) it - 1.days else it }
    val searchStart = time + searchPreWindow.first.minutes
    val searchEnd = time + searchPreWindow.last.minutes
    val addedBranches = mutableSetOf<Route>()
    return buildList {
        for ((i, e) in this@currentEntry.withIndex()) {
            val r = e.route
            if (!addedBranches.contains(r)) {
                val start: LocalDateTime
                val end: LocalDateTime
                when (e) {
                    is TimetableSingleEntry -> {
                        e.time.nextLocalDateTimeAfter(compareTime).let {
                            start = it - singleEntryWindow
                            end = it
                        }
                    }
                    is TimetableIntervalEntry -> {
                        start = e.start.nextLocalDateTimeAfter(compareTime)
                        end = e.end.nextLocalDateTimeAfter(start)
                    }
                    is TimetableSpecialEntry -> continue
                }
                if (searchEnd >= start && searchStart <= end) {
                    add(i)
                    if (size > maxResults) break
                    addedBranches.add(r)
                }
            }
        }
    }
}

private fun String.parseLocalTime(): LocalTime {
    val hour = (substring(0, 2).toInt() % 24)
    val minute = substring(2, 4).toInt()
    return LocalTime(hour, minute)
}

private fun String.parseInterval(): Int {
    return toInt() / 60
}

val dayServiceMidnight: LocalTime = LocalTime(3, 0)
val nightServiceMidnight: LocalTime = LocalTime(15, 0)

private fun LocalTime.minIntervalMinutes(other: LocalTime): Int {
    val minutes1 = hour * 60 + minute
    val minutes2 = other.hour * 60 + other.minute
    val forwardDiff = if (minutes2 - minutes1 < 0) (minutes2 - minutes1 + 1440) else (minutes2 - minutes1)
    val backwardDiff = if (minutes1 - minutes2 < 0) (minutes1 - minutes2 + 1440) else (minutes1 - minutes2)
    return min(forwardDiff, backwardDiff)
}

enum class ServiceTimeCategory(
    val day: Boolean = false,
    val night: Boolean = false
) {
    DAY(
        day = true,
    ),
    NIGHT(
        night = true
    ),
    TWENTY_FOUR_HOURS(
        day = true,
        night = true
    )
}

fun RouteTimetable.getServiceTimeCategory(): ServiceTimeCategory {
    return DayOfWeek.entries.asSequence().mapNotNull { weekday ->
        asSequence().firstOrNull { it.key.contains(weekday) }?.value
    }.flatten().toList().getServiceTimeCategory(routeNumber, co)
}

fun Collection<TimetableEntry>.getServiceTimeCategory(routeNumber: String, co: Operator): ServiceTimeCategory {
    if (co.isBus && (routeNumber.firstOrNull() == 'N' || routeNumber.lastOrNull() == 'N')) {
        return ServiceTimeCategory.NIGHT
    }
    val filtered = filter { it.hasScheduledServices }
    if (filtered.isEmpty()) {
        return ServiceTimeCategory.DAY
    }
    if (filtered.all { it.within(LocalTime(0, 0), LocalTime(6, 0)) }) {
        return ServiceTimeCategory.NIGHT
    }
    if (
        filtered.any { it.numberOfServicesWithin(LocalTime(7, 0), LocalTime(19, 59)) { true } > 0 } &&
        filtered.any { it.numberOfServicesWithin(LocalTime(2, 0), LocalTime(3, 59)) { true } > 0 }
    ) {
        return ServiceTimeCategory.TWENTY_FOUR_HOURS
    }
    val dayTimeServices = filtered.sumOf { it.numberOfServicesWithin(LocalTime(5, 0), LocalTime(0, 59)) { true } }
    val nightTimeServices = filtered.sumOf { it.numberOfServicesWithin(LocalTime(1, 0), LocalTime(4, 59)) { true } }
    return if (nightTimeServices > dayTimeServices) ServiceTimeCategory.NIGHT else ServiceTimeCategory.DAY
}

fun RouteTimetable.getRouteProportions(
    excludeWeekendsForDailyServices: Boolean = true
): Map<Route, Float> {
    val byWeekday = DayOfWeek.entries
        .associateWith { weekday -> asSequence().filter { it.key.contains(weekday) }.firstOrNull()?.value?: emptyList() }
    val dropWeekend = excludeWeekendsForDailyServices && byWeekday.any { (d, e) -> !d.isWeekend && e.isNotEmpty() }
    val entries = byWeekday.asSequence()
        .filter { !dropWeekend || !it.key.isWeekend }
        .flatMap { it.value }
        .toList()
    val firstServiceCheckTimes = entries.asSequence().mapNotNull { e -> when (e) {
        is TimetableSingleEntry -> e.time
        is TimetableIntervalEntry -> e.end
        else -> null
    } }.toSet()
    val grouped = entries.groupBy { it.route }
    val totalBuses = grouped.values.asSequence()
        .flatten()
        .sumOf { it.numberOfServices { time -> !firstServiceCheckTimes.contains(time) } }
    if (totalBuses == 0) return grouped.keys.associateWith { 1F }
    return grouped.asSequence().associate { (branch, entries) ->
        branch to entries.sumOf { it.numberOfServices { time -> !firstServiceCheckTimes.contains(time) } } / totalBuses.toFloat()
    }
}

class TimetableEntryMapBuilder(
    private val defaultRoute: Route
) {

    private val timetableEntryMap: MutableMap<DayOfWeek, MutableList<TimetableEntry>> = mutableMapOf()
    private val routeOrder: MutableSet<Route> = linkedSetOf()

    private fun interface ConflictResolver {
        fun ConflictResolver.invoke(a: TimetableEntry, b: TimetableEntry): List<TimetableEntry>
    }
    private inline fun ConflictResolver.invoke(a: TimetableEntry, b: TimetableEntry): List<TimetableEntry> = invoke(a, b)

    private fun List<TimetableEntry>.resolveConflict(
        conflictResolver: ConflictResolver
    ): List<TimetableEntry> {
        if (isEmpty()) return this
        val result = toMutableList()
        var lastEntry: TimetableEntry = result.first()
        var index = 0
        while (index < result.size - 1) {
            index++
            val currentEntry = result[index]
            if (lastEntry.overlap(currentEntry)) {
                index--
                result.removeAt(index)
                result.removeAt(index)
                val resolved = conflictResolver.invoke(lastEntry, currentEntry)
                result.addAll(index, resolved)
                index += resolved.size - 1
            }
            lastEntry = result[index]
        }
        return result
    }

    fun insert(weekdays: OperatingWeekdays, timetableEntries: List<TimetableEntry>) {
        for (weekday in weekdays) {
            timetableEntryMap.getOrPut(weekday) { mutableListOf() }.addAll(timetableEntries)
        }
        timetableEntries.forEach { routeOrder.add(it.route) }
    }

    private fun mergeSimilarTimeIntervals(entries: MutableList<TimetableEntry>) {
        for (i in (entries.size - 1) downTo 1) {
            val currentEntry = entries[i]
            if (currentEntry is TimetableIntervalEntry) {
                val previousEntry = entries[i - 1]
                if (previousEntry is TimetableIntervalEntry && currentEntry.route == previousEntry.route && currentEntry.start == previousEntry.end && currentEntry.interval maxDifference previousEntry.interval <= 5) {
                    entries.removeAt(i)
                    entries[i - 1] = previousEntry.mergeAfter(currentEntry)
                }
            }
        }
    }

    fun build(): RouteTimetable {
        val routeNumber = defaultRoute.routeNumber
        val co = defaultRoute.co.firstCo()!!
        if (timetableEntryMap.isEmpty()) {
            return routeTimetableOf(routeNumber, co, OperatingWeekdays.ALL to listOf(element = TimetableSpecialEntry(defaultRoute, "只在特定日子提供服務/沒有時間表資訊" withEn "Service only on specific days / No timetable info", null)))
        }
        for (timetableEntries in timetableEntryMap.values) {
            mergeSimilarTimeIntervals(timetableEntries)
        }
        val merged: MutableMap<OperatingWeekdays, List<TimetableEntry>> = mutableMapOf()
        var current: Pair<MutableSet<DayOfWeek>, List<TimetableEntry>>? = null
        for ((weekday, timetableEntries) in timetableEntryMap) {
            when {
                current == null -> current = mutableSetOf(weekday) to timetableEntries
                current.second == timetableEntries -> current.first.add(weekday)
                else -> {
                    merged[OperatingWeekdays(current.first)] = current.second
                    current = mutableSetOf(weekday) to timetableEntries
                }
            }
        }
        if (current?.first?.isNotEmpty() == true) {
            merged[OperatingWeekdays(current.first)] = current.second
        }
        val compareMidnight = if (merged.values.flatten().getServiceTimeCategory(routeNumber, co).day) dayServiceMidnight else nightServiceMidnight
        return RouteTimetable(routeNumber, co, merged.mapValues { (_, v) -> v.sortedWith { self, other -> when {
            self.compareTime == null -> -1
            other.compareTime == null -> 1
            else -> self.compareTime.compareToBy(other.compareTime, compareMidnight)
        } }.resolveConflict { a, b ->
            val (firstEntry, secondEntry) = if (routeOrder.indexOf { it == a.route } > routeOrder.indexOf { it == b.route }) b to a else a to b
            if (firstEntry.route.co.firstCo() == Operator.KMB && firstEntry.route.serviceType == secondEntry.route.serviceType) {
                when (firstEntry) {
                    is TimetableSingleEntry -> listOf(element = firstEntry)
                    is TimetableIntervalEntry -> {
                        if (secondEntry is TimetableIntervalEntry) {
                            buildList {
                                @Suppress("UNCHECKED_CAST")
                                val subEntries = if (firstEntry.subEntries.size > 1 || secondEntry.subEntries.size > 1) {
                                    (firstEntry.subEntries + secondEntry.subEntries).sortedBy { it.start }.resolveConflict(this@resolveConflict) as List<TimetableIntervalEntry>
                                } else {
                                    null
                                }
                                if (firstEntry.start != secondEntry.start && firstEntry.start.isBetweenInclusive(secondEntry.start, secondEntry.end)) {
                                    add(TimetableIntervalEntry(secondEntry.route, secondEntry.start, firstEntry.start, secondEntry.interval, secondEntry.specialRouteRemark, subEntries))
                                }
                                add(firstEntry)
                                if (firstEntry.end != secondEntry.end && firstEntry.end.isBetweenInclusive(secondEntry.start, secondEntry.end)) {
                                    add(TimetableIntervalEntry(secondEntry.route, firstEntry.end, secondEntry.end, secondEntry.interval, secondEntry.specialRouteRemark, subEntries))
                                }
                            }
                        } else {
                            listOf(element = firstEntry)
                        }
                    }
                    else -> listOf(element = firstEntry)
                }
            } else {
                listOf(a, b)
            }
        } })
    }

}

inline fun buildTimetableEntryMap(
    defaultRoute: Route,
    builder: (TimetableEntryMapBuilder).() -> Unit
): RouteTimetable = TimetableEntryMapBuilder(defaultRoute).apply(builder).build()

fun Collection<Route>.createTimetable(context: AppContext): RouteTimetable {
    return createTimetable(Registry.getInstance(context).getServiceDayMap()) {
        it.resolveSpecialRemark(context).takeIf { r -> r.isNotBlank() }
    }
}

fun Collection<Route>.createTimetable(serviceDayMap: Map<String, List<String>?>, resolveSpecialRemark: (Route) -> BilingualText?): RouteTimetable {
    return cache("createTimetable", this, serviceDayMap, resolveSpecialRemark) {
        buildTimetableEntryMap(first()) {
            forEach {
                it.freq?.let { f ->
                    val remark = resolveSpecialRemark.invoke(it)
                    f.forEach { (k, v) ->
                        val weekdays = OperatingWeekdays.fromBinaryStrings(serviceDayMap[k]!!)
                        val entries = v.map { (start, list) ->
                            if (list == null || start == list[0]) {
                                TimetableSingleEntry(it, start.parseLocalTime(), remark)
                            } else {
                                TimetableIntervalEntry(it, start.parseLocalTime(), list[0].parseLocalTime(), list[1].parseInterval().asRange(), remark)
                            }
                        }
                        insert(weekdays, entries)
                    }
                }
            }
        }
    }
}

operator fun <T> Map<OperatingWeekdays, T>.get(week: DayOfWeek): T? {
    return asSequence().firstOrNull { it.key.contains(week) }?.value
}

fun Collection<Route>.isTimetableActive(time: LocalDateTime, context: AppContext): Boolean {
    val registry = Registry.getInstance(context)
    return isTimetableActive(time, registry.getServiceDayMap(), registry.getHolidays())
}

fun Collection<Route>.isTimetableActive(time: LocalDateTime, serviceDayMap: Map<String, List<String>?>, holidays: Collection<LocalDate>): Boolean {
    if (isEmpty()) throw IllegalArgumentException("Route list is empty")
    val timetable = createTimetable(serviceDayMap) { null }
    val compareMidnight = if (timetable.getServiceTimeCategory().day) dayServiceMidnight else nightServiceMidnight
    val weekday = time.dayOfWeek(holidays, compareMidnight)
    val entries = timetable.entries.firstOrNull { (k) -> k.contains(weekday) }?.value?: return false
    val jt = maxOf { it.journeyTimeCircular?: 0 }
    return entries.currentEntry(timetable.routeNumber, timetable.co, time, 60, (-jt)..60, 1).isNotEmpty()
}

fun Collection<Route>.currentFirstActiveBranch(time: LocalDateTime, context: AppContext, resolveSpecialRemark: Boolean = true): List<Route> {
    val registry = Registry.getInstance(context)
    val resolveRemark: (Route) -> BilingualText? = if (resolveSpecialRemark) ({ it.resolveSpecialRemark(context).takeIf { r -> r.isNotBlank() } }) else ({ null })
    return currentFirstActiveBranch(time, registry.getServiceDayMap(), registry.getHolidays(), resolveRemark)
}

fun Collection<Route>.currentFirstActiveBranch(time: LocalDateTime, serviceDayMap: Map<String, List<String>?>, holidays: Collection<LocalDate>, resolveSpecialRemark: (Route) -> BilingualText?): List<Route> {
    if (isEmpty()) throw IllegalArgumentException("Route list is empty")
    if (size == 1) return toList()
    val timetable = createTimetable(serviceDayMap, resolveSpecialRemark)
    val compareMidnight = if (timetable.getServiceTimeCategory().day) dayServiceMidnight else nightServiceMidnight
    val weekday = time.dayOfWeek(holidays, compareMidnight)
    val entries = timetable.entries.firstOrNull { (k) -> k.contains(weekday) }?.value?: return toList()
    val current = entries.currentEntry(timetable.routeNumber, timetable.co, time).takeIf { it.isNotEmpty() }?: return toList()
    return entries.asSequence()
        .filterIndexed { i, _ -> current.contains(i) }
        .sortedBy { indexOfOrNull(it.route)?: Int.MAX_VALUE }
        .map { it.route }
        .toList()
}

enum class RouteBranchStatus(
    val activeness: Int
) {
    SOON_BEGIN(1),
    ACTIVE(4),
    HOUR_GAP(2),
    LAST_LEFT_TERMINUS(3),
    INACTIVE(0),
    NO_TIMETABLE(0)
}

fun Collection<Route>.currentBranchStatus(time: LocalDateTime, context: AppContext, resolveSpecialRemark: Boolean = true): Map<Route, RouteBranchStatus> {
    val registry = Registry.getInstance(context)
    val resolveRemark: (Route) -> BilingualText? = if (resolveSpecialRemark) ({ it.resolveSpecialRemark(context).takeIf { r -> r.isNotBlank() } }) else ({ null })
    return currentBranchStatus(time, registry.getServiceDayMap(), registry.getHolidays(), resolveRemark)
}

fun Collection<Route>.currentBranchStatus(time: LocalDateTime, serviceDayMap: Map<String, List<String>?>, holidays: Collection<LocalDate>, resolveSpecialRemark: (Route) -> BilingualText?): Map<Route, RouteBranchStatus> {
    if (isEmpty()) return emptyMap()
    val timetable = createTimetable(serviceDayMap, resolveSpecialRemark)
    val compareMidnight = if (timetable.getServiceTimeCategory().day) dayServiceMidnight else nightServiceMidnight
    val weekday = time.dayOfWeek(holidays, compareMidnight)
    val entries = timetable.entries.firstOrNull { (k) -> k.contains(weekday) }?.value?: return associateWith { RouteBranchStatus.INACTIVE }
    if (entries.all { it is TimetableSpecialEntry }) return associateWith { RouteBranchStatus.NO_TIMETABLE }
    val active = entries.currentEntry(timetable.routeNumber, timetable.co, time, 10).asSequence().map { entries[it].route }.toSet()
    val leftTerminus = filterToSet { !active.contains(it) && it.journeyTime != null && entries.currentEntry(timetable.routeNumber, timetable.co, time, 0, (-it.journeyTimeCircular!!)..0).let { d -> d.any { i -> entries[i].route == it } } }
    val soonBegin = filterToSet { !active.contains(it) && entries.currentEntry(timetable.routeNumber, timetable.co, time, 0, 0..60).let { d -> d.any { i -> entries[i].route == it } } }
    return associateWith { when {
        active.contains(it) -> RouteBranchStatus.ACTIVE
        leftTerminus.contains(it) && soonBegin.contains(it) -> if (entries.all { e -> e.route != it || e is TimetableSingleEntry }) RouteBranchStatus.ACTIVE else RouteBranchStatus.HOUR_GAP
        leftTerminus.contains(it) -> RouteBranchStatus.LAST_LEFT_TERMINUS
        soonBegin.contains(it) -> RouteBranchStatus.SOON_BEGIN
        timetable.entries.any { e -> e.value.any { e1 -> e1.route == it } } -> RouteBranchStatus.INACTIVE
        else -> RouteBranchStatus.NO_TIMETABLE
    } }
}