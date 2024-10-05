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

package com.loohp.hkbuseta.app

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.RemarkType
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.isCircular
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.TimetableIntervalEntry
import com.loohp.hkbuseta.common.utils.TimetableSingleEntry
import com.loohp.hkbuseta.common.utils.TimetableSpecialEntry
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentEntry
import com.loohp.hkbuseta.common.utils.currentMinuteState
import com.loohp.hkbuseta.common.utils.dayOfWeek
import com.loohp.hkbuseta.common.utils.dayServiceMidnight
import com.loohp.hkbuseta.common.utils.getServiceTimeCategory
import com.loohp.hkbuseta.common.utils.nightServiceMidnight
import com.loohp.hkbuseta.common.utils.toDisplayText
import com.loohp.hkbuseta.compose.ArrowDropDown
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.fontScaledDp
import kotlinx.collections.immutable.ImmutableList


private val specialDenoteChars = listOf("*", "^", "#")

fun specialDenoteChar(index: Int): String {
    return specialDenoteChars[index % specialDenoteChars.size].repeat((index / specialDenoteChars.size) + 1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableInterface(instance: AppActiveContext, routes: ImmutableList<Route>) {
    val now by currentMinuteState.collectAsStateMultiplatform()
    val timetableData = remember(routes) { routes.createTimetable(instance) }
    val compareMidnight = remember(timetableData) { if (timetableData.getServiceTimeCategory().day) dayServiceMidnight else nightServiceMidnight }
    val weekday by remember(timetableData, compareMidnight) { derivedStateOf { now.dayOfWeek(Registry.getInstance(instance).getHolidays(), compareMidnight) } }
    val timetableKeysSorted by remember(timetableData) { derivedStateOf { timetableData.keys.sorted() } }
    val timetableCurrentEntries by remember(timetableData) { derivedStateOf { timetableKeysSorted.associateWith { if (it.contains(weekday)) timetableData[it]!!.currentEntry(now) else emptyList() } } }
    val currentBranches by remember(timetableData) { derivedStateOf { timetableCurrentEntries.asSequence().map { (k, v) -> timetableData[k]!!.asSequence().filterIndexed { i, _ -> v.contains(i) }.map { it.route } }.flatten().toSet() } }
    val scroll = rememberScrollState()
    val hasJourneyTimeBranches by remember(routes) { derivedStateOf { routes.asSequence().filter { it.journeyTime != null }.associateWith { it.journeyTime!! } } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScrollWithScrollbar(
                state = scroll,
                flingBehavior = ScrollableDefaults.flingBehavior(),
                scrollbarConfig = ScrollBarConfig(
                    indicatorThickness = 4.dp
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (
            modifier = Modifier.padding(top = 25.dp, bottom = 60.dp, start = 50.dp, end = 50.dp)
        ) {
            if (hasJourneyTimeBranches.isNotEmpty()) {
                val distinctTimes = hasJourneyTimeBranches.values.distinct()
                if (distinctTimes.size <= 1) {
                    val circular = if (hasJourneyTimeBranches.keys.any { it.isCircular }) (if (Shared.language == "en") " (One way)" else " (單向)") else ""
                    PlatformText(
                        modifier = Modifier.iosExtraHeight(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold.takeIf { currentBranches.isNotEmpty() },
                        text = if (Shared.language == "en") "Full Journey Time: ${distinctTimes.first()} Min.$circular" else "全程車程: ${distinctTimes.first()}分鐘$circular"
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PlatformText(
                            modifier = Modifier.iosExtraHeight(),
                            fontSize = 16.sp,
                            text = if (Shared.language == "en") "Full Journey Times" else "全程車程"
                        )
                        PlatformText(
                            modifier = Modifier
                                .iosExtraHeight()
                                .weight(1F),
                            textAlign = TextAlign.End,
                            fontSize = 16.sp,
                            text = if (Shared.language == "en") "Times (Min.)" else "車程(分鐘)"
                        )
                    }
                    HorizontalDivider()
                    for ((route, journeyTime) in hasJourneyTimeBranches) {
                        val remark = route.resolveSpecialRemark(instance, RemarkType.LABEL_MAIN_BRANCH)
                        val weight = FontWeight.Bold.takeIf { currentBranches.contains(route) }
                        val circular = if (route.isCircular) (if (Shared.language == "en") " (One way)" else " (單向)") else ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PlatformText(
                                modifier = Modifier
                                    .iosExtraHeight()
                                    .weight(1F),
                                fontSize = 16.sp,
                                fontWeight = weight,
                                textAlign = TextAlign.Start,
                                text = "${remark[Shared.language]}$circular"
                            )
                            PlatformText(
                                modifier = Modifier
                                    .iosExtraHeight()
                                    .padding(start = 5.dp),
                                fontSize = 16.sp,
                                fontWeight = weight,
                                text = journeyTime.toString()
                            )
                        }
                    }
                }
            }
            var denoteIndex = 0
            val remarks = timetableData.values.asSequence()
                .flatten()
                .filter { it.specialRouteRemark != null }
                .groupBy { it.specialRouteRemark!! }
                .mapValues { it.value to denoteIndex++ }
            val onlyDaily by remember { derivedStateOf { timetableKeysSorted.size <= 1 } }
            timetableKeysSorted.forEach { weekdays ->
                var dayExpanded by remember { mutableStateOf(onlyDaily || composePlatform.hasLargeScreen || weekdays.contains(weekday)) }
                val dayDropdownIconDegree by animateFloatAsState(
                    targetValue = if (dayExpanded) 180F else 0F,
                    animationSpec = tween(300)
                )
                val entries = timetableData[weekdays]!!
                val anyIntervals = entries.any { it is TimetableIntervalEntry }
                val currentIndexes = timetableCurrentEntries[weekdays]!!
                var boxWidth by remember { mutableStateOf(0) }
                var leftWidth by remember { mutableStateOf(0) }
                var rightWidth by remember { mutableStateOf(0) }
                var settled by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .applyIf(!onlyDaily) { clickable { dayExpanded = !dayExpanded } }
                        .onSizeChanged { boxWidth = it.width }
                        .applyIf(settled) { animateContentSize() },
                    contentAlignment = Alignment.TopStart
                ) {
                    val style = FontWeight.Bold.takeIf { weekdays.contains(weekday) && currentIndexes.isNotEmpty() }
                    Row(
                        modifier = Modifier.onSizeChanged { leftWidth = it.width },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformText(
                            modifier = Modifier.iosExtraHeight(),
                            textAlign = TextAlign.Start,
                            fontSize = 16.sp,
                            fontWeight = style,
                            text = weekdays.displayText[Shared.language]
                        )
                        if (!onlyDaily) {
                            PlatformIcon(
                                modifier = Modifier
                                    .rotate(dayDropdownIconDegree)
                                    .size(20.fontScaledDp),
                                painter = PlatformIcons.Filled.ArrowDropDown,
                                contentDescription = if (Shared.language == "en") "Expand/Contract" else "展開/收縮"
                            )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        visible = anyIntervals && dayExpanded
                    ) {
                        PlatformText(
                            modifier = Modifier
                                .iosExtraHeight()
                                .onSizeChanged { rightWidth = it.width }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val flow = if (leftWidth + rightWidth > boxWidth) placeable.height else 0
                                    layout(placeable.width, placeable.height + flow) {
                                        placeable.placeRelative(0, flow)
                                    }.apply {
                                        settled = true
                                    }
                                },
                            textAlign = TextAlign.End,
                            fontSize = 16.sp,
                            fontWeight = style,
                            maxLines = 1,
                            text = if (Shared.language == "en") "Interval (Min.)" else "班次(分鐘)"
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize()
                ) {
                    if (dayExpanded) {
                        val timetableDisplayEntries = mutableListOf<TimetableDisplayEntries>()
                        for ((index, entry) in entries.withIndex()) {
                            val style = SpanStyle(fontWeight = FontWeight.Bold.takeIf { currentIndexes.contains(index) })
                            val stars = (entry.specialRouteRemark?.let { specialDenoteChar(remarks[it]!!.second) }?: "").asAnnotatedString(style)
                            when (entry) {
                                is TimetableIntervalEntry -> {
                                    val currentSubIndexes by remember(entry) { derivedStateOf { if (weekdays.contains(weekday)) entry.subEntries.currentEntry(now) else emptyList() } }
                                    val subDisplayEntries: List<TimetableDisplayEntries>? = if (entry.subEntries.size > 1) {
                                        buildList {
                                            for ((subIndex, subEntry) in entry.subEntries.withIndex()) {
                                                val subStyle = SpanStyle(fontWeight = FontWeight.Bold.takeIf { currentSubIndexes.contains(subIndex) })
                                                add(
                                                    TimetableDisplayEntries(
                                                        timeString = stars + subEntry.toString(Shared.language).asAnnotatedString(subStyle),
                                                        intervalString = subEntry.interval.toDisplayText().asAnnotatedString(subStyle)
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                    timetableDisplayEntries.add(
                                        TimetableDisplayEntries(
                                            timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                            intervalString = entry.interval.toDisplayText().asAnnotatedString(style),
                                            subDisplayEntries = subDisplayEntries
                                        )
                                    )
                                }
                                is TimetableSingleEntry -> {
                                    if (anyIntervals || timetableDisplayEntries.lastOrNull()?.intervalString?.isNotEmpty() == true) {
                                        timetableDisplayEntries.add(
                                            TimetableDisplayEntries(
                                                timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                                intervalString = "".asAnnotatedString()
                                            )
                                        )
                                    } else {
                                        val lastEntry = timetableDisplayEntries.removeLastOrNull()
                                        val lastString = lastEntry?.timeString?.let { it + ", ".asAnnotatedString() }?: "".asAnnotatedString()
                                        timetableDisplayEntries.add(
                                            TimetableDisplayEntries(
                                                timeString = lastString + stars + entry.toString(Shared.language).asAnnotatedString(style),
                                                intervalString = "".asAnnotatedString()
                                            )
                                        )
                                    }
                                }
                                is TimetableSpecialEntry -> {
                                    timetableDisplayEntries.add(
                                        TimetableDisplayEntries(
                                            timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                            intervalString = " ".asAnnotatedString()
                                        )
                                    )
                                }
                            }
                        }
                        for (entry in timetableDisplayEntries) {
                            var expanded by remember { mutableStateOf(false) }
                            val dropdownIconDegree by animateFloatAsState(
                                targetValue = if (expanded) 180F else 0F,
                                animationSpec = tween(300)
                            )
                            Column(
                                modifier = Modifier
                                    .applyIf(entry.subDisplayEntries != null) { clickable { expanded = !expanded } }
                                    .animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.wrapContentSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlatformText(
                                            modifier = Modifier.iosExtraHeight(),
                                            fontSize = 16.sp,
                                            text = entry.timeString
                                        )
                                        if (entry.subDisplayEntries != null) {
                                            PlatformIcon(
                                                modifier = Modifier
                                                    .rotate(dropdownIconDegree)
                                                    .size(20.fontScaledDp),
                                                painter = PlatformIcons.Filled.ArrowDropDown,
                                                contentDescription = if (Shared.language == "en") "Expand/Contract" else "展開/收縮"
                                            )
                                        }
                                    }
                                    PlatformText(
                                        modifier = Modifier.iosExtraHeight(),
                                        fontSize = 16.sp,
                                        text = entry.intervalString
                                    )
                                }
                                if (expanded && entry.subDisplayEntries != null) {
                                    for (subEntry in entry.subDisplayEntries) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 20.fontScaledDp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            PlatformText(
                                                modifier = Modifier.iosExtraHeight(),
                                                fontSize = 14.sp,
                                                text = subEntry.timeString
                                            )
                                            PlatformText(
                                                modifier = Modifier.iosExtraHeight(),
                                                fontSize = 14.sp,
                                                text = subEntry.intervalString
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(25.dp))
            for ((remark, pair) in remarks) {
                val (entries, denote) = pair
                PlatformText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosExtraHeight(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold.takeIf { entries.any { e -> currentBranches.contains(e.route) } },
                    text = specialDenoteChar(denote) + remark[Shared.language]
                )
            }
        }
    }
}

@Immutable
data class TimetableDisplayEntries(
    val timeString: AnnotatedString,
    val intervalString: AnnotatedString,
    val subDisplayEntries: List<TimetableDisplayEntries>? = null
)

private fun Modifier.iosExtraHeight(): Modifier {
    return applyIf(composePlatform.applePlatform) { padding(vertical = 2.dp) }
}
