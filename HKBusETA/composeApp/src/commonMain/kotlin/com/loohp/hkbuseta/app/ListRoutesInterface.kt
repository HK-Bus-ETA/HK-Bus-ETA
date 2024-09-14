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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.newScreenGroup
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.GeneralDirection
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.RouteSortPreference
import com.loohp.hkbuseta.common.objects.SpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.bilingualOnlyToPrefix
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.bySortModes
import com.loohp.hkbuseta.common.objects.displayName
import com.loohp.hkbuseta.common.objects.endOfLineText
import com.loohp.hkbuseta.common.objects.extendedDisplayName
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.getDisplayFormattedName
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getFare
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.getListDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getSpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.identifyGeneralDirections
import com.loohp.hkbuseta.common.objects.isDefault
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.prependTo
import com.loohp.hkbuseta.common.objects.resolvedDestFormatted
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranchFormatted
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.BoldStyle
import com.loohp.hkbuseta.common.utils.Colored
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableMap
import com.loohp.hkbuseta.common.utils.asImmutableSet
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.firstIsInstanceOrNull
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.common.utils.transformColors
import com.loohp.hkbuseta.compose.ArrowUpward
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.Close
import com.loohp.hkbuseta.compose.DoubleArrow
import com.loohp.hkbuseta.compose.LineEndCircle
import com.loohp.hkbuseta.compose.NoTransfer
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformDropdownMenu
import com.loohp.hkbuseta.compose.PlatformDropdownMenuItem
import com.loohp.hkbuseta.compose.PlatformFloatingActionButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.Schedule
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Sort
import com.loohp.hkbuseta.compose.applyIfNotNull
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.loadingPlaceholder
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformComponentBackgroundColor
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformTopBarColor
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.compose.verticalScrollBar
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.renderedSize
import com.loohp.hkbuseta.utils.routeSortPreferenceStateSaver
import io.ktor.util.collections.ConcurrentMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random


@OptIn(ExperimentalCoroutinesApi::class)
private val etaUpdateScope: CoroutineDispatcher = dispatcherIO.limitedParallelism(8)

private val etaColor: Color @Composable get() = if (Shared.theme.isDarkMode) Color(0xFFAAC3D5) else Color(0xFF2582C4)
private val etaSecondColor: Color @Composable get() = if (Shared.theme.isDarkMode) Color(0xFFCCCCCC) else Color(0xFF444444)

@Immutable
data class RouteListScrollPosition(
    val routes: ImmutableList<StopIndexedRouteSearchResultEntry>,
    val itemIndex: Int,
    val itemOffset: Int
)

private val scrollPositions: MutableMap<RouteListType, RouteListScrollPosition> = ConcurrentMutableMap()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRoutesInterface(
    instance: AppActiveContext,
    routes: ImmutableList<StopIndexedRouteSearchResultEntry>,
    checkSpecialDest: Boolean,
    listType: RouteListType,
    showEta: Boolean,
    recentSort: RecentSortMode,
    proximitySortOrigin: Coordinates?,
    showEmptyText: Boolean = true,
    visible: Boolean = true,
    maintainScrollPosition: Boolean = true,
    bottomExtraSpace: Dp = 0.dp,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    reorderable: (suspend CoroutineScope.(LazyListItemInfo, LazyListItemInfo) -> Unit)? = null,
    onPullToRefresh: (suspend () -> Unit)? = null
) {
    val routeSortPreferenceProvider by remember(listType, recentSort, proximitySortOrigin) { derivedStateOf { {
        if (recentSort.forcedMode) {
            RouteSortPreference(recentSort.defaultSortMode, false)
        } else {
            val preference = Shared.routeSortModePreference[listType]
            val routeSortMode = preference?.routeSortMode?.let { if (it.isLegalMode(
                    allowRecentSort = recentSort == RecentSortMode.CHOICE,
                    allowProximitySort = proximitySortOrigin != null
                )) it else null }?: RouteSortMode.NORMAL
            val filterTimetableActive = preference?.filterTimetableActive == true
            RouteSortPreference(routeSortMode, filterTimetableActive)
        }
    } } }
    val activeSortModeState = rememberSaveable(saver = routeSortPreferenceStateSaver) { mutableStateOf(routeSortPreferenceProvider.invoke()) }

    var routeGroupedByDirections: Map<GeneralDirection, List<StopIndexedRouteSearchResultEntry>> by remember { mutableStateOf(emptyMap()) }
    val filterDirectionsState: MutableState<GeneralDirection?> = remember { mutableStateOf(null) }
    val filterDirections by filterDirectionsState
    val filterRoutes = remember(routes, routeGroupedByDirections, filterDirections) {
        if (routeGroupedByDirections.isEmpty() || filterDirections == null) {
            routes
        } else {
            routeGroupedByDirections[filterDirections]?.asImmutableList()?: persistentListOf()
        }
    }

    val pullToRefreshState = if (onPullToRefresh == null) null else rememberPullToRefreshState()

    LaunchedEffect (routes) {
        CoroutineScope(dispatcherIO).launch {
            val byDirections = routes.identifyGeneralDirections(instance).takeIf { it.size > 1 }?: emptyMap()
            withContext(Dispatchers.Main) {
                routeGroupedByDirections = byDirections
            }
        }
    }
    LaunchedEffect (visible, routeSortPreferenceProvider) {
        activeSortModeState.value = routeSortPreferenceProvider.invoke()
    }

    Scaffold(
        modifier = Modifier.background(platformComponentBackgroundColor),
        topBar = {
            ListRouteTopBar(
                instance = instance,
                routesEmpty = filterRoutes.isEmpty(),
                listType = listType,
                recentSort = recentSort,
                filterDirectionsState = filterDirectionsState,
                availableDirections = routeGroupedByDirections.keys.asImmutableSet(),
                proximitySortOrigin = proximitySortOrigin,
                extraActions = extraActions,
                activeSortModeState = activeSortModeState
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .applyIfNotNull(pullToRefreshState) { nestedScroll(it.nestedScrollConnection) }
                    .padding(padding)
            ) {
                if (routes.isEmpty()) {
                    EmptyListRouteInterface(
                        instance = instance,
                        showEta = showEta,
                        recentSort = recentSort,
                        showEmptyText = showEmptyText
                    )
                } else {
                    ListRouteInterfaceInternal(
                        instance = instance,
                        routes = filterRoutes,
                        checkSpecialDest = checkSpecialDest,
                        listType = listType,
                        showEta = showEta,
                        recentSort = recentSort,
                        proximitySortOrigin = proximitySortOrigin,
                        maintainScrollPosition = maintainScrollPosition,
                        bottomExtraSpace = bottomExtraSpace,
                        reorderable = reorderable,
                        activeSortModeState = activeSortModeState,
                        filterDirectionsState = filterDirectionsState
                    )
                }
                if (pullToRefreshState != null) {
                    if (pullToRefreshState.isRefreshing) {
                        LaunchedEffect(true) {
                            onPullToRefresh?.invoke()
                            pullToRefreshState.endRefresh()
                        }
                    }
                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                    )
                }
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRouteTopBar(
    instance: AppActiveContext,
    routesEmpty: Boolean,
    listType: RouteListType,
    recentSort: RecentSortMode,
    filterDirectionsState: MutableState<GeneralDirection?>,
    availableDirections: ImmutableSet<GeneralDirection>,
    proximitySortOrigin: Coordinates?,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    activeSortModeState: MutableState<RouteSortPreference>
) {
    var activeSortMode by activeSortModeState
    var filterDirections by filterDirectionsState
    val scope = rememberCoroutineScope()
    Box (
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = recentSort == RecentSortMode.CHOICE || proximitySortOrigin != null || extraActions != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            var expandSortModeDropdown by remember { mutableStateOf(false) }
            var expandFilterDirectionDropdown by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .platformHorizontalDividerShadow(5.dp)
                    .background(platformTopBarColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                var sortModeButtonSize by remember { mutableStateOf(IntSize(0, 10)) }
                var filterDirectionButtonSize by remember { mutableStateOf(IntSize(0, 10)) }
                if (extraActions != null || availableDirections.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .height(max(filterDirectionButtonSize.height, sortModeButtonSize.height).equivalentDp)
                    ) {
                        extraActions?.invoke(this)
                        if (availableDirections.isNotEmpty()) {
                            Box {
                                PlatformButton(
                                    modifier = Modifier
                                        .plainTooltip(if (Shared.language == "en") "Filter by Direction" else "按方向過濾")
                                        .onSizeChanged { filterDirectionButtonSize = it },
                                    onClick = { expandFilterDirectionDropdown = true },
                                    contentPadding = PaddingValues(10.dp),
                                    shape = platformLargeShape,
                                    colors = ButtonDefaults.textButtonColors(),
                                    content = {
                                        PlatformIcon(
                                            modifier = Modifier.size(23.dp),
                                            painter = PlatformIcons.Outlined.DoubleArrow,
                                            contentDescription = filterDirections.displayName[Shared.language]
                                        )
                                        PlatformText(
                                            fontSize = 20.sp,
                                            maxLines = 1,
                                            text = filterDirections.displayName[Shared.language]
                                        )
                                    }
                                )
                                Box {
                                    Spacer(modifier = Modifier.size(filterDirectionButtonSize.height.equivalentDp))
                                    PlatformDropdownMenu(
                                        expanded = expandFilterDirectionDropdown,
                                        onDismissRequest = { expandFilterDirectionDropdown = false }
                                    ) {
                                        PlatformText(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            fontSize = 17.sp,
                                            color = platformLocalContentColor.adjustAlpha(0.5F),
                                            text = if (Shared.language == "en") "Filter by Direction" else "按方向過濾"
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 7.dp))
                                        for (direction in setOf(null) + availableDirections) {
                                            PlatformDropdownMenuItem(
                                                onClick = {
                                                    expandFilterDirectionDropdown = false
                                                    filterDirections = direction
                                                },
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(5.sp.dp, Alignment.Start),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(
                                                            selected = direction == filterDirections,
                                                            onClick = null
                                                        )
                                                        PlatformText(
                                                            fontSize = 20.sp,
                                                            text = direction.extendedDisplayName[Shared.language]
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(sortModeButtonSize.width.equivalentDp))
                    }
                }
                PlatformButton(
                    modifier = Modifier
                        .plainTooltip(if (Shared.language == "en") "Sorting" else "排序")
                        .onSizeChanged { sortModeButtonSize = it },
                    onClick = { expandSortModeDropdown = true },
                    contentPadding = PaddingValues(10.dp),
                    shape = platformLargeShape,
                    colors = ButtonDefaults.textButtonColors(),
                    enabled = !routesEmpty && (recentSort == RecentSortMode.CHOICE || proximitySortOrigin != null),
                    content = {
                        PlatformIcon(
                            modifier = Modifier.size(23.dp),
                            painter = PlatformIcons.AutoMirrored.Filled.Sort,
                            contentDescription = activeSortMode.routeSortMode.sortPrefixedTitle[Shared.language]
                        )
                        PlatformText(
                            fontSize = 20.sp,
                            maxLines = 1,
                            text = activeSortMode.routeSortMode.title[Shared.language]
                        )
                    }
                )
                Box {
                    Spacer(modifier = Modifier.size(sortModeButtonSize.height.equivalentDp))
                    PlatformDropdownMenu(
                        expanded = expandSortModeDropdown,
                        onDismissRequest = { expandSortModeDropdown = false }
                    ) {
                        PlatformText(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            fontSize = 17.sp,
                            color = platformLocalContentColor.adjustAlpha(0.5F),
                            text = if (Shared.language == "en") "Sorting" else "排序"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 7.dp))
                        for (mode in RouteSortMode.entries) {
                            if (mode.isLegalMode(recentSort == RecentSortMode.CHOICE, proximitySortOrigin != null)) {
                                PlatformDropdownMenuItem(
                                    onClick = {
                                        expandSortModeDropdown = false
                                        scope.launch {
                                            activeSortMode = activeSortMode.copy(routeSortMode = mode)
                                            Shared.routeSortModePreference[listType].let {
                                                if (activeSortMode != it) {
                                                    Registry.getInstance(instance).setRouteSortModePreference(instance, listType, mode)
                                                }
                                            }
                                        }
                                    },
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(5.sp.dp, Alignment.Start),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = activeSortMode.routeSortMode == mode,
                                                onClick = null
                                            )
                                            PlatformText(
                                                fontSize = 20.sp,
                                                text = mode.extendedTitle[Shared.language]
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 7.dp))
                        PlatformDropdownMenuItem(
                            onClick = {
                                scope.launch {
                                    activeSortMode = activeSortMode.copy(filterTimetableActive = !activeSortMode.filterTimetableActive)
                                    Shared.routeSortModePreference[listType].let {
                                        if (activeSortMode != it) {
                                            Registry.getInstance(instance).setRouteSortModePreference(instance, listType, activeSortMode)
                                        }
                                    }
                                }
                            },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.sp.dp, Alignment.Start),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = activeSortMode.filterTimetableActive,
                                        onCheckedChange = null
                                    )
                                    PlatformText(
                                        fontSize = 20.sp,
                                        text = if (Shared.language == "en") "Prioritize In Service" else "現正服務優先"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListRouteInterface(
    instance: AppActiveContext,
    showEta: Boolean,
    recentSort: RecentSortMode,
    showEmptyText: Boolean
) {
    if (showEmptyText) {
        EmptyBackgroundInterface(
            instance = instance,
            icon = PlatformIcons.Filled.NoTransfer,
            text = if (Shared.language == "en") "No Routes to Display" else "沒有可顯示的路線"
        )
    } else {
        val count = remember { Random.nextInt(3, 9) }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 0 until count) {
                val route = remember { Registry.getInstance(instance).randomRoute() }
                val stop = remember { if (Random.nextBoolean()) Registry.getInstance(instance).randomStop() else null }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (if (showEta) 65 else 55).dp)
                        .padding(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.requiredWidth(92.5F.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.loadingPlaceholder(true),
                            textAlign = TextAlign.Start,
                            fontSize = 30F.sp,
                            maxLines = 1,
                            text = route.routeNumber
                        )
                        PlatformText(
                            modifier = Modifier.loadingPlaceholder(true),
                            textAlign = TextAlign.Start,
                            fontSize = 14F.sp,
                            maxLines = 1,
                            text = route.co.firstCo()?.getDisplayName(route.routeNumber, route.isKmbCtbJoint, route.gmbRegion, Shared.language)?: ""
                        )
                    }
                    Column (
                        modifier = Modifier.weight(1F),
                    ) {
                        PlatformText(
                            modifier = Modifier.loadingPlaceholder(true),
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            lineHeight = 1.1F.em,
                            fontSize = 22F.sp,
                            maxLines = 1,
                            text = route.dest.prependTo()[Shared.language]
                        )
                        if (stop != null) {
                            PlatformText(
                                modifier = Modifier.loadingPlaceholder(true),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                fontSize = 16F.sp,
                                maxLines = 1,
                                text = stop.name[Shared.language]
                            )
                        }
                    }
                    if (recentSort == RecentSortMode.FORCED || showEta) {
                        Box(
                            modifier = Modifier
                                .size(50F.dp)
                                .loadingPlaceholder(true),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListRouteInterfaceInternal(
    instance: AppActiveContext,
    routes: ImmutableList<StopIndexedRouteSearchResultEntry>,
    checkSpecialDest: Boolean,
    listType: RouteListType,
    showEta: Boolean,
    recentSort: RecentSortMode,
    proximitySortOrigin: Coordinates?,
    maintainScrollPosition: Boolean,
    bottomExtraSpace: Dp,
    reorderable: (suspend CoroutineScope.(LazyListItemInfo, LazyListItemInfo) -> Unit)?,
    activeSortModeState: MutableState<RouteSortPreference>,
    filterDirectionsState: MutableState<GeneralDirection?>
) {
    val haptics = LocalHapticFeedback.current
    val initialScrollPosition = remember { scrollPositions[listType]?.takeIf { it.routes == routes } }
    val scroll = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollPosition?.itemIndex?: 0,
        initialFirstVisibleItemScrollOffset = initialScrollPosition?.itemOffset?: 0
    )
    val scope = rememberCoroutineScope()
    val lastLookupRoutes by if (listType == RouteListType.RECENT) Shared.lastLookupRoutes.collectAsStateMultiplatform() else remember { mutableStateOf(emptyList()) }
    val reorderableState = rememberReorderableLazyListState(scroll, onMove = reorderable?: { _, _ -> })

    var filterDirections by filterDirectionsState
    var activeSortMode by activeSortModeState
    val sortedByModeProvider = { routes.bySortModes(instance, recentSort, listType != RouteListType.RECENT, activeSortMode.filterTimetableActive, proximitySortOrigin).asImmutableMap() }
    var sortedByMode by remember { mutableStateOf(sortedByModeProvider.invoke()) }
    val sortedResults by remember(sortedByMode, activeSortMode) { derivedStateOf { sortedByMode[activeSortMode.routeSortMode]?: routes } }
    var init by remember { mutableStateOf(false) }

    LaunchedEffect (routes, lastLookupRoutes, listType, proximitySortOrigin, activeSortMode.filterTimetableActive) {
        delay(100)
        CoroutineScope(dispatcherIO).launch {
            val sorted = sortedByModeProvider.invoke()
            withContext(Dispatchers.Main) {
                sortedByMode = sorted
            }
        }
    }
    LaunchedEffect (scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset) {
        scrollPositions[listType] = RouteListScrollPosition(
            routes = routes,
            itemIndex = scroll.firstVisibleItemIndex,
            itemOffset = scroll.firstVisibleItemScrollOffset
        )
    }
    LaunchedEffect (routes, sortedByMode) {
        if (init) {
            delay(100)
            if (!maintainScrollPosition) {
                scroll.scrollToItem(0)
            }
        } else {
            delay(100)
            init = true
        }
    }
    LaunchedEffect (lastLookupRoutes) {
        delay(100)
        if (scroll.firstVisibleItemIndex in 0..1) {
            scroll.animateScrollToItem(0)
        }
    }
    var previousSortFilter by remember { mutableStateOf(activeSortMode to filterDirections) }
    ChangedEffect (sortedResults) {
        scope.launch {
            delay(100)
            if (previousSortFilter != activeSortMode to filterDirections && (scroll.firstVisibleItemIndex > 0 || scroll.firstVisibleItemScrollOffset > 0)) {
                scroll.animateScrollToItem(0)
            }
            previousSortFilter = activeSortMode to filterDirections
        }
    }

    val etaResults: MutableMap<String, Registry.ETAQueryResult> = remember { ConcurrentMap() }
    val etaUpdateTimes: MutableMap<String, Long> = remember { ConcurrentMap() }

    val etaResultsState = remember { etaResults.asImmutableState() }
    val etaUpdateTimesState = remember { etaUpdateTimes.asImmutableState() }

    val routeNumberWidth by if (Shared.language == "en") "249M".renderedSize(30F.sp) else "機場快線".renderedSize(22F.sp)
    val reorderEnabled by remember(reorderable, activeSortMode, filterDirections) { derivedStateOf { reorderable != null && activeSortMode.isDefault && filterDirections == null } }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .verticalScrollBar(
                    state = scroll,
                    scrollbarConfig = ScrollBarConfig(
                        indicatorThickness = 6.dp,
                        padding = PaddingValues(0.dp, 2.dp, 0.dp, 2.dp)
                    )
                ),
            state = scroll,
            contentPadding = PaddingValues(vertical = 1.dp)
        ) {
            itemsIndexed(sortedResults, key = { _, route -> route.uniqueKey }) { index, route ->
                val uniqueKey = route.uniqueKey
                val width = routeNumberWidth?.width?: 0
                val deleteFunction = { Registry.getInstance(instance).removeLastLookupRoutes(route.routeKey, instance) }.takeIf { recentSort == RecentSortMode.FORCED }
                if (reorderEnabled) {
                    ReorderableItem(
                        state = reorderableState,
                        enabled = reorderEnabled,
                        key = uniqueKey
                    ) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 2.dp else 0.dp)
                        RouteEntry(
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    enabled = reorderEnabled,
                                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                                )
                                .shadow(elevation),
                            key = uniqueKey,
                            listType = listType,
                            routeNumberWidth = width,
                            showEta = showEta,
                            deleteFunction = deleteFunction,
                            onLongClick = null,
                            route = route,
                            checkSpecialDest = checkSpecialDest,
                            etaResults = etaResultsState,
                            etaUpdateTimes = etaUpdateTimesState,
                            instance = instance
                        )
                    }
                } else {
                    RouteEntry(
                        modifier = Modifier,
                        key = uniqueKey,
                        listType = listType,
                        routeNumberWidth = width,
                        showEta = showEta,
                        deleteFunction = deleteFunction,
                        onLongClick = if (reorderable != null) ({
                            instance.compose.showToastText(
                                text = if (Shared.language == "en") {
                                    "Routes may only be reordered while no sorting or filters are enabled"
                                } else {
                                    "未使用排序或過濾時才可以對路線進行重新排序"
                                },
                                duration = ToastDuration.LONG,
                                actionLabel = if (Shared.language == "en") {
                                    "Reset"
                                } else {
                                    "重置排序及過濾"
                                },
                                action = {
                                    scope.launch {
                                        activeSortMode = RouteSortPreference.DEFAULT
                                        filterDirections = null
                                        Shared.routeSortModePreference[listType].let {
                                            if (activeSortMode != it) {
                                                Registry.getInstance(instance).setRouteSortModePreference(instance, listType, activeSortMode)
                                            }
                                        }
                                    }
                                }
                            )
                        }) else null,
                        route = route,
                        checkSpecialDest = checkSpecialDest,
                        etaResults = etaResultsState,
                        etaUpdateTimes = etaUpdateTimesState,
                        instance = instance
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.graphicsLayer { translationY = if (index + 1 >= sortedResults.size) 1.dp.toPx() else 0F }
                )
            }
            if (bottomExtraSpace > 0.dp) {
                item {
                    Spacer(modifier = Modifier.size(bottomExtraSpace))
                }
            }
        }
        val offset by animateDpAsState(
            targetValue = if (scroll.firstVisibleItemIndex >= 10) 0.dp else (-100).dp,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )
        if (offset > (-100).dp) {
            PlatformFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(45.dp)
                    .graphicsLayer { translationY = offset.toPx() }
                    .plainTooltip(if (Shared.language == "en") "Scroll to Top" else "返回頂部"),
                onClick = { scope.launch { scroll.animateScrollToItem(0) } },
            ) {
                PlatformIcon(
                    modifier = Modifier.size(27.dp),
                    painter = PlatformIcons.Filled.ArrowUpward,
                    contentDescription = if (Shared.language == "en") "Scroll to Top" else "返回頂部"
                )
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.RouteEntry(
    modifier: Modifier,
    key: String,
    listType: RouteListType,
    routeNumberWidth: Int,
    showEta: Boolean,
    deleteFunction: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    route: StopIndexedRouteSearchResultEntry,
    checkSpecialDest: Boolean,
    etaResults: ImmutableState<out MutableMap<String, Registry.ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>,
    instance: AppActiveContext
) {
    Box(
        modifier = modifier
            .fillParentMaxWidth()
            .animateItemPlacement()
            .background(platformComponentBackgroundColor)
            .combinedClickable(
                onClick = {
                    CoroutineScope(dispatcherIO).launch {
                        Registry.getInstance(instance).addLastLookupRoute(route.routeKey, instance)
                        val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                        intent.putExtra("route", route)
                        if (route.stopInfo != null) {
                            intent.putExtra("stopId", route.stopInfo!!.stopId)
                        }
                        if (HistoryStack.historyStack.value.last().newScreenGroup() == AppScreenGroup.ROUTE_STOPS) {
                            instance.startActivity(AppIntent(instance, AppScreen.DUMMY))
                            delay(300)
                        }
                        instance.startActivity(intent)
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        RouteRow(
            key = key,
            listType = listType,
            routeNumberWidth = routeNumberWidth,
            showEta = showEta,
            deleteFunction = deleteFunction,
            route = route,
            checkSpecialDest = checkSpecialDest,
            etaResults = etaResults,
            etaUpdateTimes = etaUpdateTimes,
            instance = instance)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteRow(
    key: String,
    listType: RouteListType,
    routeNumberWidth: Int,
    showEta: Boolean,
    deleteFunction: (() -> Unit)?,
    route: StopIndexedRouteSearchResultEntry,
    checkSpecialDest: Boolean,
    etaResults: ImmutableState<out MutableMap<String, Registry.ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>,
    instance: AppActiveContext
) {
    val co = route.co
    val kmbCtbJoint = route.route!!.isKmbCtbJoint
    val routeNumber = route.route!!.routeNumber
    val routeNumberDisplay = co.getListDisplayRouteNumber(routeNumber, true)
    val dest = route.resolvedDestFormatted(false, instance, *if (Shared.disableBoldDest) emptyArray() else arrayOf(BoldStyle))[Shared.language].asContentAnnotatedString().annotatedString
    var specialRouteAlerts: Set<SpecialRouteAlerts>? by remember { mutableStateOf(null) }
    val gmbRegion = route.route!!.gmbRegion
    val secondLineCoColor = co.getColor(routeNumber, Color.White).adjustBrightness(if (Shared.theme.isDarkMode) 1F else 0.7F)
    val localContentColor = LocalContentColor.current
    val secondLine = remember(route, co, kmbCtbJoint, routeNumber, dest, secondLineCoColor, listType, localContentColor) { buildList {
        if (listType == RouteListType.RECENT) {
            add(instance.formatDateTime((Shared.findLookupRouteTime(route.routeKey)?: 0).toLocalDateTime(), true).asAnnotatedString(SpanStyle(color = localContentColor.adjustAlpha(0.6F))))
        }
        if (route.stopInfo != null) {
            add(buildAnnotatedString {
                append(route.stopInfo!!.data!!.name[Shared.language])
                append("", SpanStyle(fontSize = TextUnit.Small))
                if (listType == RouteListType.NEARBY) {
                    val distance = (route.stopInfo!!.distance * 1000).roundToInt()
                    append("  $distance${if (Shared.language == "en") "m" else "米"}", SpanStyle(fontSize = TextUnit.Small))
                }
                val fare = route.route!!.getFare(route.stopInfoIndex, Registry.getInstance(instance).isPublicHoliday(currentLocalDateTime().date))
                append(fare?.let { "  $$it" }?: "", SpanStyle(fontSize = TextUnit.Small))
            })
        }
        if (co == Operator.NLB || co.isFerry) {
            add((if (Shared.language == "en") "From ${route.route!!.orig.en}" else "從${route.route!!.orig.zh}開出").asAnnotatedString(SpanStyle(color = secondLineCoColor)))
        } else if (co == Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
            add((if (Shared.language == "en") "Sun Bus (NR$routeNumber)" else "陽光巴士 (NR$routeNumber)").asAnnotatedString(SpanStyle(color = secondLineCoColor)))
        }
    } }
    val otherDests = specialRouteAlerts?.firstIsInstanceOrNull<SpecialRouteAlerts.SpecialDest>()?.routes?.mapNotNull {
        if (it == route.route) {
            null
        } else {
            it to it.resolvedDestWithBranchFormatted(false, it, route.stopInfoIndex, route.stopInfo!!.stopId, instance, *if (Shared.disableBoldDest) emptyArray() else arrayOf(BoldStyle))[Shared.language].asContentAnnotatedString().annotatedString
        }
    }

    LaunchedEffect (route) {
        if (checkSpecialDest) {
            CoroutineScope(dispatcherIO).launch {
                val alerts = route.route!!.getSpecialRouteAlerts(instance)
                withContext(Dispatchers.Main) { specialRouteAlerts = alerts }
            }
        }
    }

    Row (
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (if (showEta) 65 else 55).dp)
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.requiredWidth(routeNumberWidth.equivalentDp),
            verticalArrangement = Arrangement.Center
        ) {
            if (specialRouteAlerts?.contains(SpecialRouteAlerts.CheckDest) == true) {
                val contentColor = platformLocalContentColor
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.sp.dp)
                        .drawBehind {
                            drawRoundRect(
                                topLeft = Offset(-3.sp.toPx(), 1.sp.toPx()),
                                size = Size(size.width + 6.sp.toPx(), size.height + 2.sp.toPx()),
                                cornerRadius = CornerRadius(4.sp.toPx()),
                                color = contentColor,
                                style = Stroke(width = 2.sp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PlatformText(
                        modifier = Modifier
                            .offset(y = (-2).sp.dp),
                        textAlign = TextAlign.Start,
                        fontSize = if ((co == Operator.MTR || co.isFerry) && Shared.language != "en") {
                            22F.sp
                        } else {
                            30F.sp
                        },
                        lineHeight = 1.1F.em,
                        maxLines = 1,
                        text = routeNumberDisplay
                    )
                    PlatformText(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 2.sp.dp),
                        textAlign = TextAlign.Start,
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        maxLines = 1,
                        text = if (Shared.language == "en") "Check Dest" else "留意目的地"
                    )
                }
            } else {
                PlatformText(
                    textAlign = TextAlign.Start,
                    fontSize = if ((co == Operator.MTR || co.isFerry) && Shared.language != "en") {
                        22F.sp
                    } else {
                        30F.sp
                    },
                    lineHeight = 1.1F.em,
                    maxLines = 1,
                    text = routeNumberDisplay
                )
            }
            PlatformText(
                textAlign = TextAlign.Start,
                fontSize = 14F.sp,
                lineHeight = 1.1F.em,
                maxLines = 1,
                text = co.getDisplayFormattedName(routeNumber, kmbCtbJoint, gmbRegion, Shared.language)
                    .let { if (Shared.theme.isDarkMode) it else it.transformColors { _, c -> Colored(Color(c.color).adjustBrightness(0.75F).toArgb().toLong()) } }
                    .asContentAnnotatedString()
                    .annotatedString
            )
        }
        if (secondLine.isEmpty() && otherDests == null) {
            Row(
                modifier = Modifier.weight(1F),
                horizontalArrangement = Arrangement.Start
            ) {
                if (route.route!!.shouldPrependTo()) {
                    PlatformText(
                        modifier = Modifier.alignByBaseline(),
                        textAlign = TextAlign.Start,
                        lineHeight = 1.1F.em,
                        fontSize = 22F.sp,
                        maxLines = 1,
                        text = bilingualToPrefix[Shared.language].asAnnotatedString(SpanStyle(fontSize = TextUnit.Small))
                    )
                }
                PlatformText(
                    modifier = Modifier
                        .alignByBaseline()
                        .weight(1F)
                        .userMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    lineHeight = 1.1F.em,
                    fontSize = 22F.sp,
                    maxLines = userMarqueeMaxLines(),
                    text = dest
                )
            }
        } else {
            val density = LocalDensity.current
            var maxHeight by remember(density.density, density.fontScale, secondLine) { mutableIntStateOf(0) }
            Column (
                modifier = Modifier
                    .weight(1F)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val height = placeable.height.coerceAtLeast(maxHeight)
                        maxHeight = height
                        layout(placeable.width, height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (route.route!!.shouldPrependTo()) {
                        PlatformText(
                            modifier = Modifier.alignByBaseline(),
                            textAlign = TextAlign.Start,
                            lineHeight = 1.1F.em,
                            fontSize = 22F.sp,
                            maxLines = 1,
                            text = bilingualToPrefix[Shared.language].asAnnotatedString(SpanStyle(fontSize = TextUnit.Small))
                        )
                    }
                    PlatformText(
                        modifier = Modifier
                            .alignByBaseline()
                            .userMarquee(),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        lineHeight = 1.1F.em,
                        fontSize = 22F.sp,
                        maxLines = userMarqueeMaxLines(),
                        text = dest
                    )
                }
                otherDests?.let { otherDests ->
                    for ((otherRoute, otherDest) in otherDests) {
                        Row(
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (otherRoute.shouldPrependTo()) {
                                PlatformText(
                                    modifier = Modifier.alignByBaseline(),
                                    textAlign = TextAlign.Start,
                                    lineHeight = 1.1F.em,
                                    fontSize = 17F.sp,
                                    maxLines = 1,
                                    text = bilingualOnlyToPrefix[Shared.language].asAnnotatedString(SpanStyle(fontSize = TextUnit.Small))
                                )
                            }
                            PlatformText(
                                modifier = Modifier
                                    .alignByBaseline()
                                    .weight(1F)
                                    .userMarquee(),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                lineHeight = 1.1F.em,
                                fontSize = 17F.sp,
                                maxLines = userMarqueeMaxLines(),
                                text = otherDest
                            )
                        }
                    }
                }
                if (secondLine.isNotEmpty()) {
                    val infiniteTransition = rememberInfiniteTransition(label = "SecondLineCrossFade")
                    val animatedCurrentLine by infiniteTransition.animateValue(
                        initialValue = 0,
                        targetValue = secondLine.size,
                        typeConverter = Int.VectorConverter,
                        animationSpec = infiniteRepeatable(
                            animation = tween(5500 * secondLine.size, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "SecondLineCrossFade"
                    )
                    Crossfade(
                        modifier = Modifier.animateContentSize(),
                        targetState = animatedCurrentLine,
                        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                        label = "SecondLineCrossFade"
                    ) {
                        PlatformText(
                            modifier = Modifier.userMarquee(),
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            fontSize = 16F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = userMarqueeMaxLines(),
                            text = secondLine[it.coerceIn(secondLine.indices)]
                        )
                    }
                }
            }
        }
        if (deleteFunction != null) {
            PlatformButton(
                modifier = Modifier
                    .size(50F.dp)
                    .clip(CircleShape)
                    .plainTooltip(if (Shared.language == "en") "Remove From History" else "從搜尋歷史中移除"),
                colors = ButtonDefaults.clearColors(),
                contentPadding = PaddingValues(0.dp),
                onClick = deleteFunction
            ) {
                PlatformIcon(
                    modifier = Modifier.size(32.5F.dp),
                    painter = PlatformIcons.Filled.Close,
                    tint = Color.Red,
                    contentDescription = if (Shared.language == "en") "Remove From History" else "從搜尋歷史中移除"
                )
            }
        } else if (showEta && route.stopInfo != null) {
            ETAElement(key, route, etaResults, etaUpdateTimes, instance)
        }
    }
}

@Composable
fun ETAElement(key: String, route: StopIndexedRouteSearchResultEntry, etaResults: ImmutableState<out MutableMap<String, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: AppActiveContext) {
    var etaState by remember { mutableStateOf(etaResults.value[key]) }

    LaunchedEffect (Unit) {
        etaUpdateTimes.value[key]?.apply {
            delay(etaUpdateTimes.value[key]?.let { (Shared.ETA_UPDATE_INTERVAL - (currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        while (true) {
            val result = CoroutineScope(etaUpdateScope).async {
                Registry.getInstance(instance).getEta(route.stopInfo!!.stopId, route.stopInfoIndex, route.co, route.route!!, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            }.await()
            etaState = result
            etaResults.value[key] = result
            if (!result.isConnectionError) {
                etaUpdateTimes.value[key] = currentTimeMillis()
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }
    RestartEffect {
        val result = CoroutineScope(etaUpdateScope).async {
            Registry.getInstance(instance).getEta(route.stopInfo!!.stopId, route.stopInfoIndex, route.co, route.route!!, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
        }.await()
        etaState = result
        etaResults.value[key] = result
        if (!result.isConnectionError) {
            etaUpdateTimes.value[key] = currentTimeMillis()
        }
    }

    Column (
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    PlatformIcon(
                        modifier = Modifier.size(30.dp),
                        painter = PlatformIcons.Outlined.LineEndCircle,
                        contentDescription = route.route!!.endOfLineText[Shared.language],
                        tint = etaColor,
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).typhoonInfo }.collectAsStateMultiplatform()
                    Image(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(DrawableResource("cyclone.png")),
                        contentDescription = typhoonInfo.typhoonWarningTitle
                    )
                } else {
                    PlatformIcon(
                        modifier = Modifier.size(30.dp),
                        painter = PlatformIcons.Outlined.Schedule,
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        tint = etaColor,
                    )
                }
            } else {
                val (text, lineHeight) = when (Shared.etaDisplayMode) {
                    ETADisplayMode.COUNTDOWN -> {
                        val (text1, text2) = eta.firstLine.shortText
                        buildAnnotatedString {
                            append(text1, SpanStyle(fontSize = 27.sp, color = etaColor))
                            append("\n")
                            append(text2, SpanStyle(fontSize = 14.sp, color = etaColor))
                            (2..3).mapNotNull {
                                val (eText1, eText2) = eta[it].shortText
                                if (eText1.isBlank() || eText2 != text2) {
                                    null
                                } else {
                                    eText1
                                }
                            }.takeIf { it.isNotEmpty() }?.joinToString(", ", postfix = text2)?.apply {
                                append("\n")
                                append(this, SpanStyle(fontSize = 11.sp))
                            }
                        } to 1.1F.em
                    }
                    ETADisplayMode.CLOCK_TIME -> {
                        val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                        buildAnnotatedString {
                            append(text1, SpanStyle(fontSize = 20.sp, color = etaColor, fontWeight = FontWeight.Bold))
                            (2..3).forEach {
                                val eText1 = eta.getResolvedText(it, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                                if (eText1.length > 1) {
                                    append("\n")
                                    append(eText1, SpanStyle(fontSize = 16.sp))
                                }
                            }
                        } to 1.4F.em
                    }
                    ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> {
                        val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                        val (text2, text3) = eta.firstLine.shortText
                        buildAnnotatedString {
                            append(text1, SpanStyle(fontSize = 20.sp, color = etaColor, fontWeight = FontWeight.Bold))
                            append("  ")
                            append(text2, SpanStyle(fontSize = 20.sp, color = etaColor, fontWeight = FontWeight.Bold))
                            append(text3, SpanStyle(fontSize = 12.sp, color = etaColor))
                            (2..3).forEach {
                                val eText1 = eta.getResolvedText(it, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                                if (eText1.length > 1) {
                                    val (eText2, eText3) = eta[it].shortText
                                    append("\n")
                                    append(eText1, SpanStyle(fontSize = 16.sp))
                                    append("  ")
                                    append(eText2, SpanStyle(fontSize = 16.sp))
                                    append(eText3, SpanStyle(fontSize = 10.sp))
                                }
                            }
                        } to 1.4F.em
                    }
                }
                PlatformText(
                    textAlign = TextAlign.End,
                    fontSize = 14.sp,
                    color = etaSecondColor,
                    lineHeight = lineHeight,
                    maxLines = 3,
                    text = text
                )
            }
        }
    }
}