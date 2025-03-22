/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.common.utils

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.shared.Registry
import kotlin.math.absoluteValue

fun createMTRLineSectionData(co: Operator, color: Long, stopList: List<Registry.StopData>, mtrStopsInterchange: List<Registry.MTRInterchangeData>, isLrtCircular: Boolean, context: AppContext): List<MTRStopSectionData> {
    val stopByBranchId: MutableMap<Route, MutableList<Registry.StopData>> = mutableMapOf()
    stopList.forEach { stop -> stop.branchIds.forEach { stopByBranchId.getOrPut(it) { mutableListOf() }.add(stop) } }
    val hasOutOfStation = mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }
    return stopList.withIndex().map { (index, stop) ->
        MTRStopSectionData.build(stop.serviceType == 1, stopByBranchId, index, stop, stopList, co, color, isLrtCircular, mtrStopsInterchange[index], hasOutOfStation, context)
    }
}

@Immutable
data class MTRStopSectionData(
    val mainLine: MTRStopSectionMainLineData?,
    val spurLine: MTRStopSectionSpurLineData?,
    val index: Int,
    val stop: Registry.StopData,
    val co: Operator,
    val color: Long,
    val isLrtCircular: Boolean,
    val interchangeData: Registry.MTRInterchangeData,
    val hasOutOfStation: Boolean,
    val stopByBranchId: MutableMap<Route, MutableList<Registry.StopData>>,
    val requireExtension: Boolean,
    val context: AppContext
) {
    companion object {
        fun build(isMainLine: Boolean, stopByBranchId: MutableMap<Route, MutableList<Registry.StopData>>, index: Int, stop: Registry.StopData, stopList: List<Registry.StopData>, co: Operator, color: Long, isLrtCircular: Boolean, interchangeData: Registry.MTRInterchangeData, hasOutOfStation: Boolean, context: AppContext): MTRStopSectionData {
            val requireExtension = index + 1 < stopList.size
            return if (isMainLine) MTRStopSectionData(MTRStopSectionMainLineData(
                isFirstStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue } <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue }.let { x -> x < 0 || x >= it.size - 1 } },
                hasOtherParallelBranches = hasOtherParallelBranches(stopList, stopByBranchId, stop),
                sideSpurLineType = getSideSpurLineType(stopList, stopByBranchId, stop)
            ), null, index, stop, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, requireExtension, context) else MTRStopSectionData(null, MTRStopSectionSpurLineData(
                hasParallelMainLine = index > 0 && index < stopList.size - 1,
                dashLineResult = isDashLineSpur(stopList, stop),
                isFirstStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue } <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue }.let { x -> x < 0 || x >= it.size - 1 } }
            ), index, stop, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, requireExtension, context)
        }
    }
}

@Immutable
data class MTRStopSectionMainLineData(
    val isFirstStation: Boolean,
    val isLastStation: Boolean,
    val hasOtherParallelBranches: Boolean,
    val sideSpurLineType: SideSpurLineType
)

@Immutable
data class MTRStopSectionSpurLineData(
    val hasParallelMainLine: Boolean,
    val dashLineResult: DashLineSpurResult,
    val isFirstStation: Boolean,
    val isLastStation: Boolean
)

private fun hasOtherParallelBranches(stopList: List<Registry.StopData>, stopByBranchId: MutableMap<Route, MutableList<Registry.StopData>>, stop: Registry.StopData): Boolean {
    if (stopByBranchId.size == stopByBranchId.filter { it.value.contains(stop) }.size) {
        return false
    }
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var branchStart = -1
    var branchStartStop: Registry.StopData? = null
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchStart = i
            branchStartStop = stopList[i]
            break
        }
    }
    if (branchStartStop == null) {
        for (i in stopList.indices) {
            if (stopList[i].branchIds != branchIds) {
                branchStart = i
                branchStartStop = stopList[i]
                break
            }
        }
    }
    var branchEnd = stopList.size
    var branchEndStop: Registry.StopData? = null
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchEnd = i
            branchEndStop = stopList[i]
            break
        }
    }
    if (branchEndStop == null) {
        for (i in (stopList.size - 1) downTo 0) {
            if (stopList[i].branchIds != branchIds) {
                branchEnd = i
                branchEndStop = stopList[i]
                break
            }
        }
    }
    val matchingBranchStart = branchStart == mainIndex
    val matchingBranchEnd = branchEnd == mainIndex
    val isStartOfSpur = matchingBranchStart && stopByBranchId.values.none { it.indexOf(branchStartStop) <= 0 }
    val isEndOfSpur = matchingBranchEnd && stopByBranchId.values.none { it.indexOf(branchEndStop) >= (it.size - 1) }
    if (matchingBranchStart != isStartOfSpur || matchingBranchEnd != isEndOfSpur) {
        return false
    }
    return mainIndex in branchStart..branchEnd
}

enum class SideSpurLineType {

    NONE, COMBINE, DIVERGE

}

private fun getSideSpurLineType(stopList: List<Registry.StopData>, stopByBranchId: MutableMap<Route, MutableList<Registry.StopData>>, stop: Registry.StopData): SideSpurLineType {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    if (mainIndex > 0) {
        if (stopList[mainIndex - 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex - 1]) || it.subList(0, it.indexOf(stopList[mainIndex - 1])).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) != 0 }) {
                return SideSpurLineType.COMBINE
            }
        }
    }
    if (mainIndex < stopList.size - 1) {
        if (stopList[mainIndex + 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex + 1]) || it.subList(it.indexOf(stopList[mainIndex + 1]) + 1, it.size).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) < it.size - 1 }) {
                return SideSpurLineType.DIVERGE
            }
        }
    }
    return SideSpurLineType.NONE
}

data class DashLineSpurResult(val value: Boolean, val isStartOfSpur: Boolean, val isEndOfSpur: Boolean) {

    companion object {

        val NOT_DASH = DashLineSpurResult(value = false, isStartOfSpur = false, isEndOfSpur = false)

    }

}

private fun isDashLineSpur(stopList: List<Registry.StopData>, stop: Registry.StopData): DashLineSpurResult {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var possible = false
    var branchStart = false
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            if (i + 1 == mainIndex && stopList[i].branchIds != branchIds) {
                branchStart = true
            }
            possible = true
            break
        }
    }
    if (!possible) {
        return DashLineSpurResult.NOT_DASH
    }
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            return DashLineSpurResult(true, branchStart, i - 1 == mainIndex && stopList[i].branchIds != branchIds)
        }
    }
    return DashLineSpurResult.NOT_DASH
}