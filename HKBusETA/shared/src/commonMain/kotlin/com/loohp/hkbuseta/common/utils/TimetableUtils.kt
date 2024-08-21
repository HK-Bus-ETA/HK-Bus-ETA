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
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.appcontext.ReduceDataPossiblyOmitted
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.joinBilingualText
import com.loohp.hkbuseta.common.objects.journeyTimeCircular
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

val LocalTime.Companion.MIDNIGHT: LocalTime by lazy { LocalTime(0, 0) }

inline fun LocalDateTime.dayOfWeek(holidays: Collection<LocalDate>, midnight: LocalTime): DayOfWeek {
    val mightNightOffset = midnight - LocalTime.MIDNIGHT
    return (this - mightNightOffset).date.dayOfWeek(holidays)
}

inline fun LocalDate.dayOfWeek(holidays: Collection<LocalDate>): DayOfWeek {
    return if (holidays.contains(this)) DayOfWeek.SUNDAY else dayOfWeek
}

inline val DayOfWeek.isWeekend: Boolean get() = when (this) {
    DayOfWeek.MONDAY -> false
    DayOfWeek.TUESDAY -> false
    DayOfWeek.WEDNESDAY -> false
    DayOfWeek.THURSDAY -> false
    DayOfWeek.FRIDAY -> false
    DayOfWeek.SATURDAY -> true
    DayOfWeek.SUNDAY -> true
    else -> false
}

class OperatingWeekdays(
    private val weekdays: Set<DayOfWeek>
): Set<DayOfWeek>, Comparable<OperatingWeekdays> {

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

    override val size: Int = weekdays.size
    override fun isEmpty(): Boolean = weekdays.isEmpty()
    override fun iterator(): Iterator<DayOfWeek> = weekdays.iterator()
    override fun containsAll(elements: Collection<DayOfWeek>): Boolean = weekdays.containsAll(elements)
    override fun contains(element: DayOfWeek): Boolean = weekdays.contains(element)

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
    abstract fun toString(language: String): String
    abstract fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean
    abstract infix fun overlap(other: TimetableEntry): Boolean
    abstract fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int
}
@Immutable
class TimetableIntervalEntry(
    route: Route,
    val start: LocalTime,
    val end: LocalTime,
    val interval: Int,
    specialRouteRemark: BilingualText?
): TimetableEntry(route, specialRouteRemark, start) {
    override fun toString(language: String): String {
        return "${start.hour.pad(2)}:${start.minute.pad(2)} - ${end.hour.pad(2)}:${end.minute.pad(2)}"
    }
    override fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean {
        return start.isBetweenInclusive(windowStart, windowEnd) && end.isBetweenInclusive(windowStart, windowEnd)
    }
    override fun overlap(other: TimetableEntry): Boolean {
        if (other !is TimetableIntervalEntry) return false
        if (start == other.start || end == other.end) return true
        if (other.start.isBetweenInclusive(start, end)) return true
        if (other.end.isBetweenInclusive(start, end)) return true
        return false
    }
    override fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int {
        return ((end - start).inWholeMinutes.toInt() / interval) + if (firstServiceCheck.invoke(start)) 1 else 0
    }
}
@Immutable
class TimetableSingleEntry(
    route: Route,
    val time: LocalTime,
    specialRouteRemark: BilingualText?
): TimetableEntry(route, specialRouteRemark, time) {
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
}
@Immutable
class TimetableSpecialEntry(
    route: Route,
    val notice: BilingualText,
    specialRouteRemark: BilingualText?
): TimetableEntry(route, specialRouteRemark, null) {
    override fun toString(language: String): String = notice[language]
    override fun within(windowStart: LocalTime, windowEnd: LocalTime): Boolean = false
    override infix fun overlap(other: TimetableEntry): Boolean = false
    override fun numberOfServices(firstServiceCheck: (LocalTime) -> Boolean): Int = 0
}

fun List<TimetableEntry>.currentEntry(
    time: LocalDateTime,
    singleEntryActivePreWindowMinutes: Int = 60,
    searchPreWindow: IntRange = 0..0,
): List<Int> {
    if (isEmpty()) return emptyList()
    if (all { it is TimetableSpecialEntry }) return listOf(0)
    val singleEntryWindow = singleEntryActivePreWindowMinutes.minutes
    val result = mutableSetOf<Int>()
    val addedBranches = mutableSetOf<Route>()
    for (padMinute in searchPreWindow) {
        val checkTime = time + padMinute.minutes
        forEachIndexed { i, e ->
            val r = e.route
            if (!addedBranches.contains(r)) {
                when (e) {
                    is TimetableSingleEntry -> {
                        if (e.time.nextLocalDateTimeAfter(checkTime) - checkTime <= singleEntryWindow) {
                            result.add(i)
                            addedBranches.add(r)
                        }
                    }
                    is TimetableIntervalEntry -> {
                        if (e.start.nextLocalDateTimeAfter(checkTime) >= e.end.nextLocalDateTimeAfter(checkTime)) {
                            result.add(i)
                            addedBranches.add(e.route)
                        }
                    }
                    is TimetableSpecialEntry -> { /* do nothing */ }
                }
            }
        }
    }
    return result.sorted()
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

fun Map<OperatingWeekdays, List<TimetableEntry>>.isNightRoute(): Boolean {
    return DayOfWeek.entries.asSequence().mapNotNull { weekday ->
        asSequence().firstOrNull { it.key.contains(weekday) }?.value
    }.flatten().toList().isNightRoute()
}

fun Collection<TimetableEntry>.isNightRoute(): Boolean {
    if (all { it is TimetableSpecialEntry }) return false
    val diff = (0 until 24).associateWith { h ->
        val time = LocalTime(h, 0)
        minOf { when (it) {
            is TimetableSingleEntry -> it.time.minIntervalMinutes(time)
            is TimetableIntervalEntry -> if (time.isBetweenInclusive(it.start, it.end)) 0 else min(it.start.minIntervalMinutes(time), it.end.minIntervalMinutes(time))
            else -> throw IllegalArgumentException("Invalid timetable entry type")
        } }
    }
    val minDiff = diff.values.min()
    return diff.asSequence().filter { it.value == minDiff }.all { it.key in 0..5 }
}

fun Map<OperatingWeekdays, List<TimetableEntry>>.getRouteProportions(
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

    private fun List<TimetableEntry>.resolveConflict(conflictResolver: (TimetableEntry, TimetableEntry) -> List<TimetableEntry>): List<TimetableEntry> {
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

    fun build(): Map<OperatingWeekdays, List<TimetableEntry>> {
        if (timetableEntryMap.isEmpty()) {
            return mapOf(OperatingWeekdays.ALL to listOf(TimetableSpecialEntry(defaultRoute, "只在特定日子提供服務" withEn "Service only on specific days", null)))
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
        val compareMidnight = if (merged.values.flatten().isNightRoute()) nightServiceMidnight else dayServiceMidnight
        return merged.mapValues { (_, v) -> v.sortedWith { self, other -> when {
            self.compareTime == null -> -1
            other.compareTime == null -> 1
            else -> self.compareTime.compareToBy(other.compareTime, compareMidnight)
        } }.resolveConflict { a, b ->
            val (firstEntry, secondEntry) = if (routeOrder.indexOf { it == a.route } > routeOrder.indexOf { it == b.route }) b to a else a to b
            if (firstEntry.route.serviceType == secondEntry.route.serviceType) {
                when (firstEntry) {
                    is TimetableSingleEntry -> listOf(firstEntry)
                    is TimetableIntervalEntry -> {
                        if (secondEntry is TimetableIntervalEntry) {
                            buildList {
                                if (firstEntry.start != secondEntry.start && firstEntry.start.isBetweenInclusive(secondEntry.start, secondEntry.end)) {
                                    add(TimetableIntervalEntry(secondEntry.route, secondEntry.start, firstEntry.start, secondEntry.interval, secondEntry.specialRouteRemark))
                                }
                                add(firstEntry)
                                if (firstEntry.end != secondEntry.end && firstEntry.end.isBetweenInclusive(secondEntry.start, secondEntry.end)) {
                                    add(TimetableIntervalEntry(secondEntry.route, firstEntry.end, secondEntry.end, secondEntry.interval, secondEntry.specialRouteRemark))
                                }
                            }
                        } else {
                            listOf(firstEntry)
                        }
                    }
                    else -> listOf(firstEntry)
                }
            } else {
                listOf(a, b)
            }
        } }
    }

}

inline fun buildTimetableEntryMap(
    defaultRoute: Route,
    builder: (TimetableEntryMapBuilder).() -> Unit
): Map<OperatingWeekdays, List<TimetableEntry>> = TimetableEntryMapBuilder(defaultRoute).apply(builder).build()

@ReduceDataOmitted
@ReduceDataPossiblyOmitted
fun Collection<Route>.createTimetable(context: AppContext): Map<OperatingWeekdays, List<TimetableEntry>> {
    return createTimetable(Registry.getInstance(context).getServiceDayMap()) {
        it.resolveSpecialRemark(context).takeIf { r -> r.zh.isNotBlank() }
    }
}

fun Collection<Route>.createTimetable(serviceDayMap: Map<String, List<String>?>, resolveSpecialRemark: (Route) -> BilingualText?): Map<OperatingWeekdays, List<TimetableEntry>> {
    return cache("createTimetable", this, serviceDayMap, resolveSpecialRemark) {
        buildTimetableEntryMap(first()) {
            forEach {
                it.freq?.let { f ->
                    val remark = resolveSpecialRemark.invoke(it)
                    f.forEach { (k, v) ->
                        val weekdays = OperatingWeekdays.fromBinaryStrings(serviceDayMap[k]!!)
                        val entries = v.map { (start, list) ->
                            when (list) {
                                null -> TimetableSingleEntry(it, start.parseLocalTime(), remark)
                                else -> TimetableIntervalEntry(it, start.parseLocalTime(), list[0].parseLocalTime(), list[1].parseInterval(), remark)
                            }
                        }
                        insert(weekdays, entries)
                    }
                }
            }
        }
    }
}

@ReduceDataOmitted
@ReduceDataPossiblyOmitted
fun Collection<Route>.currentFirstActiveBranch(time: LocalDateTime, context: AppContext): Route {
    val registry = Registry.getInstance(context)
    return currentFirstActiveBranch(time, registry.getServiceDayMap(), registry.getHolidays()) {
        it.resolveSpecialRemark(context).takeIf { r -> r.zh.isNotBlank() }
    }
}

fun Collection<Route>.currentFirstActiveBranch(time: LocalDateTime, serviceDayMap: Map<String, List<String>?>, holidays: Collection<LocalDate>, resolveSpecialRemark: (Route) -> BilingualText?): Route {
    if (isEmpty()) throw IllegalArgumentException("Route list is empty")
    if (size == 1) return first()
    val timetable = createTimetable(serviceDayMap, resolveSpecialRemark)
    val compareMidnight = if (timetable.isNightRoute()) nightServiceMidnight else dayServiceMidnight
    val weekday = time.dayOfWeek(holidays, compareMidnight)
    val entries = timetable.entries.firstOrNull { (k, _) -> k.contains(weekday) }?.value?: return first()
    val current = entries.currentEntry(time).takeIf { it.isNotEmpty() }?: return first()
    return entries.asSequence()
        .filterIndexed { i, _ -> current.contains(i) }
        .sortedBy { indexOfOrNull(it.route)?: Int.MAX_VALUE }
        .first().route
}

enum class RouteBranchStatus {
    SOON_BEGIN, ACTIVE, HOUR_GAP, LAST_LEFT_TERMINUS, INACTIVE, NO_TIMETABLE
}

@ReduceDataOmitted
@ReduceDataPossiblyOmitted
fun Collection<Route>.currentBranchStatus(time: LocalDateTime, context: AppContext): Map<Route, RouteBranchStatus> {
    val registry = Registry.getInstance(context)
    return currentBranchStatus(time, registry.getServiceDayMap(), registry.getHolidays()) {
        it.resolveSpecialRemark(context).takeIf { r -> r.zh.isNotBlank() }
    }
}

fun Collection<Route>.currentBranchStatus(time: LocalDateTime, serviceDayMap: Map<String, List<String>?>, holidays: Collection<LocalDate>, resolveSpecialRemark: (Route) -> BilingualText?): Map<Route, RouteBranchStatus> {
    if (isEmpty()) return emptyMap()
    val timetable = createTimetable(serviceDayMap, resolveSpecialRemark)
    val compareMidnight = if (timetable.isNightRoute()) nightServiceMidnight else dayServiceMidnight
    val weekday = time.dayOfWeek(holidays, compareMidnight)
    val entries = timetable.entries.firstOrNull { (k, _) -> k.contains(weekday) }?.value?: return associateWith { RouteBranchStatus.INACTIVE }
    if (entries.all { it is TimetableSpecialEntry }) return associateWith { RouteBranchStatus.NO_TIMETABLE }
    val active = entries.currentEntry(time, 10).asSequence().map { entries[it].route }.toSet()
    val leftTerminus = filterToSet { !active.contains(it) && it.journeyTime != null && entries.currentEntry(time, 0, (-it.journeyTimeCircular!!)..0).let { d -> d.any { i -> entries[i].route == it } } }
    val soonBegin = filterToSet { !active.contains(it) && entries.currentEntry(time, 0, 0..60).let { d -> d.any { i -> entries[i].route == it } } }
    return associateWith { when {
        active.contains(it) -> RouteBranchStatus.ACTIVE
        leftTerminus.contains(it) && soonBegin.contains(it) -> if (entries.all { e -> e.route != it || e is TimetableSingleEntry }) RouteBranchStatus.ACTIVE else RouteBranchStatus.HOUR_GAP
        leftTerminus.contains(it) -> RouteBranchStatus.LAST_LEFT_TERMINUS
        soonBegin.contains(it) -> RouteBranchStatus.SOON_BEGIN
        else -> RouteBranchStatus.INACTIVE
    } }
}