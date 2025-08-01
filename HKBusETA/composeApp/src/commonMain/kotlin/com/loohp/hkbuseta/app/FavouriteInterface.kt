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

package com.loohp.hkbuseta.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.anyEquals
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.asFormattedText
import com.loohp.hkbuseta.common.objects.asOriginData
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.getOperatorName
import com.loohp.hkbuseta.common.objects.identifyStopCo
import com.loohp.hkbuseta.common.objects.indexOfName
import com.loohp.hkbuseta.common.objects.isDefaultGroup
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.stop
import com.loohp.hkbuseta.common.objects.toRouteSearchResult
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.awaitWithTimeout
import com.loohp.hkbuseta.compose.Add
import com.loohp.hkbuseta.compose.AdvanceTabRow
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.DeleteDialog
import com.loohp.hkbuseta.compose.DeleteForever
import com.loohp.hkbuseta.compose.Edit
import com.loohp.hkbuseta.compose.EditNote
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.IconPainterLayer
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.NearMe
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformTab
import com.loohp.hkbuseta.compose.PlatformTabRow
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.Reorder
import com.loohp.hkbuseta.compose.Route
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Signal
import com.loohp.hkbuseta.compose.TextInputDialog
import com.loohp.hkbuseta.compose.TransferWithinAStation
import com.loohp.hkbuseta.compose.WrongLocation
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.dummySignal
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformComponentBackgroundColor
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.rememberAutoResizeTextState
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.coordinatesNullableStateSaver
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.lastLocation
import com.loohp.hkbuseta.utils.renderedSizes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableColumn


data class FavouriteTabItem(
    val title: BilingualText,
    val iconLayers: List<IconPainterLayer>
) {
    constructor(
        title: BilingualText,
        icon: @Composable () -> Painter
    ): this(
        title = title,
        iconLayers = listOf(IconPainterLayer(icon))
    )
}

val mainFavouriteTabItem = listOf(
    FavouriteTabItem(
        title = "路線" withEn "Routes",
        icon = { PlatformIcons.Outlined.Route }
    ),
    FavouriteTabItem(
        title = "巴士站" withEn "Stops",
        icon = { painterResource(DrawableResource("bus_stop_vector.xml")) }
    )
)

@Composable
fun FavouriteInterface(instance: AppActiveContext, visible: Boolean = true, signal: Signal = dummySignal) {
    val pagerState = rememberPagerState(
        initialPage = Shared.viewFavTab.coerceIn(0, mainFavouriteTabItem.size - 1),
        pageCount = { mainFavouriteTabItem.size }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect (pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val page = pagerState.currentPage
            CoroutineScope(Dispatchers.IO).launch {
                Registry.getInstance(instance).setViewFavTab(page, instance)
            }
        }
    }
    ChangedEffect (signal) {
        val index = pagerState.currentPage
        scope.launch { pagerState.animateScrollToPage(if (index == 0) 1 else 0) }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PlatformTabRow(selectedTabIndex = pagerState.currentPage) {
            mainFavouriteTabItem.forEachIndexed { index, (title, iconLayers) ->
                PlatformTab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        Box {
                            for ((icon, overrideColor, modifier) in iconLayers) {
                                PlatformIcon(
                                    modifier = Modifier.size(18F.sp.dp).run(modifier),
                                    painter = icon.invoke(),
                                    tint = overrideColor,
                                    contentDescription = title[Shared.language]
                                )
                            }
                        }
                    },
                    text = {
                        PlatformText(
                            fontSize = 16F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = title[Shared.language]
                        )
                    }
                )
            }
        }
        HorizontalPager(
            modifier = Modifier.weight(1F),
            state = pagerState,
            userScrollEnabled = true
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val isOnPage = pagerState.currentPage == it && visible
                when (it) {
                    0 -> FavouriteRouteStopInterface(instance, isOnPage)
                    1 -> FavouriteStopInterface(instance, isOnPage)
                    else -> PlatformText(mainFavouriteTabItem[it].title[Shared.language])
                }
            }
        }
    }
}

private val favouriteRouteStopChosenTabState: MutableStateFlow<Int> = MutableStateFlow(0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteRouteStopInterface(instance: AppActiveContext, visible: Boolean) {
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateMultiplatform()
    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(lastLocation?.location) }

    LaunchedEffect (visible) {
        if (visible) {
            val fastResult = getGPSLocation(instance, LocationPriority.FASTER).awaitWithTimeout(3000)
            if (fastResult?.isSuccess == true) {
                location = fastResult.location!!
            }
            while (true) {
                val result = getGPSLocation(instance).await()
                if (result?.isSuccess == true) {
                    location = result.location!!
                }
                delay(Shared.ETA_UPDATE_INTERVAL.toLong())
            }
        }
    }

    var favouriteRouteStopChosenTab by favouriteRouteStopChosenTabState.collectAsStateMultiplatform()
    val pagerState = rememberPagerState(initialPage = favouriteRouteStopChosenTab) { favouriteRouteStops.size }
    val scope = rememberCoroutineScope()
    val routes: MutableMap<BilingualText, ImmutableList<StopIndexedRouteSearchResultEntry>> = remember { mutableStateMapOf() }

    LaunchedEffect (pagerState.currentPage) {
        favouriteRouteStopChosenTab = pagerState.currentPage
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val tabScrollable = remember(favouriteRouteStops) { mutableStateMapOf<Int, Boolean>() }
        val tabTexts = remember(favouriteRouteStops) { favouriteRouteStops.asSequence().map { it.name[Shared.language] }.toImmutableList() }
        val renderSizes = tabTexts.renderedSizes(16F.sp, spanStyle = SpanStyle(fontWeight = FontWeight.Bold))
        AdvanceTabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = pagerState.currentPage,
            totalTabSize = pagerState.pageCount,
            scrollable = tabScrollable.values.any { it },
            widestTabWidth = renderSizes.values.maxOfOrNull { it.size.width }?.equivalentDp?.plus(20.dp)?: 1.dp
        ) { scrollable ->
            favouriteRouteStops.forEachIndexed { index, item ->
                PlatformTab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        PlatformText(
                            modifier = Modifier
                                .fillMaxWidth(0.9F)
                                .padding(horizontal = 10.dp),
                            onTextLayout = {
                                if (!scrollable) {
                                    tabScrollable[index] = it.didOverflowWidth || it.didOverflowHeight
                                }
                            },
                            fontSize = 16F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = item.name[Shared.language]
                        )
                    }
                )
            }
        }
        val sheetState = rememberPlatformModalBottomSheetState()
        var editingRouteGroup by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect (editingRouteGroup) {
            ScreenState.hasInterruptElement.value = editingRouteGroup
        }
        HorizontalPager(
            modifier = Modifier.weight(1F),
            state = pagerState,
            userScrollEnabled = true
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val isOnPage by remember(it, visible) { derivedStateOf { pagerState.currentPage == it && visible } }
                val favouriteRouteStop by remember(it) { derivedStateOf { favouriteRouteStops[it] } }
                LaunchedEffect (favouriteRouteStop, isOnPage) {
                    val (name, routeStops) = favouriteRouteStop
                    if (isOnPage) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val routeSearch = routeStops.toRouteSearchResult(instance, location).toStopIndexed(instance).asImmutableList()
                            withContext(Dispatchers.Main) {
                                routes[name] = routeSearch
                            }
                        }
                    }
                }
                val (name) = favouriteRouteStop
                val editActionButton: @Composable RowScope.() -> Unit = {
                    PlatformButton(
                        onClick = { editingRouteGroup = true },
                        contentPadding = PaddingValues(10.dp),
                        shape = platformLargeShape,
                        colors = ButtonDefaults.textButtonColors(),
                        content = {
                            PlatformIcon(
                                modifier = Modifier.size(27.dp),
                                painter = PlatformIcons.Filled.EditNote,
                                contentDescription = if (Shared.language == "en") "Edit Favourite Route Stop Groups" else "編輯收藏路線巴士站分類"
                            )
                            PlatformText(
                                fontSize = 20.sp,
                                maxLines = 1,
                                text = if (Shared.language == "en") "Edit Groups" else "編輯分類"
                            )
                        }
                    )
                }
                Box (
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    routes[name]?.let { route ->
                        ListRoutesInterface(
                            instance = instance,
                            routes = route,
                            checkSpecialDest = true,
                            listType = RouteListType.FAVOURITE,
                            showEta = true,
                            recentSort = RecentSortMode.DISABLED,
                            proximitySortOrigin = location?.asOriginData(false),
                            proximitySortOriginIsRealLocation = true,
                            showEmptyText = visible,
                            visible = visible,
                            extraActions = editActionButton,
                            reorderable = { from, to ->
                                val groups = Shared.favoriteRouteStops.value.toMutableList()
                                val groupIndex = groups.indexOfName(favouriteRouteStop.name)
                                val newList = groups[groupIndex].let { (name, list) -> FavouriteRouteGroup(name, list.toMutableList().apply { add(to.index, removeAt(from.index)) }) }
                                groups[groupIndex] = newList
                                routes[name] = newList.favouriteRouteStops.toRouteSearchResult(instance, location).toStopIndexed(instance).asImmutableList()
                                CoroutineScope(Dispatchers.IO).launch {
                                    Registry.getInstance(instance).setFavouriteRouteGroups(groups, instance)
                                }
                            },
                            pipModeListName = name.asFormattedText()
                        )
                    }?: run {
                        ListRoutesInterface(
                            instance = instance,
                            routes = persistentListOf(),
                            checkSpecialDest = true,
                            listType = RouteListType.FAVOURITE,
                            showEta = true,
                            recentSort = RecentSortMode.DISABLED,
                            proximitySortOrigin = location?.asOriginData(false),
                            proximitySortOriginIsRealLocation = true,
                            showEmptyText = false,
                            visible = visible,
                            extraActions = editActionButton,
                            pipModeListName = name.asFormattedText()
                        )
                    }
                }
                if (editingRouteGroup) {
                    val actionScroll = rememberScrollState()
                    var deleting: BilingualText? by remember { mutableStateOf(null) }
                    var renaming: String? by remember { mutableStateOf(null) }
                    var adding by remember { mutableStateOf(false) }
                    LaunchedEffect (deleting, renaming) {
                        ScreenState.hasInterruptElement.value = deleting != null || renaming != null
                    }
                    PlatformModalBottomSheet(
                        onDismissRequest = { editingRouteGroup = false },
                        sheetState = sheetState
                    ) {
                        Scaffold(
                            topBar = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .platformHorizontalDividerShadow(5.dp)
                                        .background(platformPrimaryContainerColor)
                                        .padding(5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    PlatformText(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = if (Shared.language == "en") "Edit Favourite Route Stop Groups" else "編輯收藏路線巴士站分類",
                                        fontSize = 25.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            },
                            content = { padding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(padding)
                                ) {
                                    ReorderableColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScrollWithScrollbar(
                                                state = actionScroll,
                                                flingBehavior = ScrollableDefaults.flingBehavior(),
                                                scrollbarConfig = ScrollBarConfig(
                                                    indicatorThickness = 4.dp
                                                )
                                            ),
                                        list = favouriteRouteStops,
                                        onSettle = { fromIndex, toIndex ->
                                            val list = favouriteRouteStops.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                                            Registry.getInstance(instance).setFavouriteRouteGroups(list, instance)
                                        }
                                    ) { _, item, isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 2.dp else 0.dp)
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            key(item) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(60.dp)
                                                        .background(platformComponentBackgroundColor)
                                                        .shadow(elevation)
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.Start),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AutoResizeText(
                                                        modifier = Modifier.weight(1F),
                                                        textAlign = TextAlign.Start,
                                                        fontSizeRange = FontSizeRange(max = 22.sp),
                                                        text = item.name[Shared.language]
                                                    )
                                                    if (!item.isDefaultGroup) {
                                                        PlatformButton(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape),
                                                            colors = ButtonDefaults.clearColors(),
                                                            contentPadding = PaddingValues(0.dp),
                                                            onClick = { deleting = item.name }
                                                        ) {
                                                            PlatformIcon(
                                                                modifier = Modifier.size(26.dp),
                                                                tint = Color.Red,
                                                                painter = PlatformIcons.Outlined.DeleteForever,
                                                                contentDescription = if (Shared.language == "en") "Delete" else "刪除"
                                                            )
                                                        }
                                                        PlatformButton(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape),
                                                            colors = ButtonDefaults.clearColors(),
                                                            contentPadding = PaddingValues(0.dp),
                                                            onClick = { renaming = item.name[Shared.language] }
                                                        ) {
                                                            PlatformIcon(
                                                                modifier = Modifier.size(26.dp),
                                                                painter = PlatformIcons.Outlined.Edit,
                                                                contentDescription = if (Shared.language == "en") "Rename" else "重新命名"
                                                            )
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .draggableHandle(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        PlatformIcon(
                                                            modifier = Modifier.size(26.dp),
                                                            painter = PlatformIcons.Outlined.Reorder,
                                                            contentDescription = if (Shared.language == "en") "Reorder" else "排序"
                                                        )
                                                    }
                                                }
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .background(platformComponentBackgroundColor),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlatformButton(
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(0.dp),
                                            colors = ButtonDefaults.clearColors(),
                                            contentPadding = PaddingValues(0.dp),
                                            onClick = { adding = true }
                                        ) {
                                            PlatformIcon(
                                                modifier = Modifier.size(26.dp),
                                                tint = platformLocalContentColor.adjustAlpha(0.5F),
                                                painter = PlatformIcons.Outlined.Add,
                                                contentDescription = if (Shared.language == "en") "New" else "新增"
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        )
                    }
                    if (renaming != null) {
                        TextInputDialog(
                            title = "編輯分類名稱" withEn "Edit Group Name",
                            initialText = renaming,
                            confirmText = "編輯" withEn "Edit",
                            inputValidation = { it.isNotBlank() && (it == renaming || favouriteRouteStops.none { g -> it anyEquals g.name }) },
                            placeholder = "名稱" withEn "Name",
                            onDismissRequest = { _, _ -> renaming = null },
                            onConfirmation = {
                                val newName = it.asBilingualText()
                                val updated = Shared.favoriteRouteStops.value.map { g ->
                                    val (n, l) = g
                                    if (n.zh == renaming) FavouriteRouteGroup(newName, l) else g
                                }
                                renaming = null
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            }
                        )
                    } else if (deleting != null) {
                        DeleteDialog(
                            icon = PlatformIcons.Filled.DeleteForever,
                            title = "確認刪除分類\n${deleting?.zh}" withEn "Confirm Removal\n${deleting?.en}",
                            text = "一經確認將不能復原" withEn "This action cannot be undone.",
                            onDismissRequest = { deleting = null },
                            onConfirmation = {
                                val deleteName = deleting
                                deleting = null
                                val updated = Shared.favoriteRouteStops.value.filter { (n, _) -> n != deleteName }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            }
                        )
                    } else if (adding) {
                        TextInputDialog(
                            title = "新增分類" withEn "New Group",
                            confirmText = "新增" withEn "Add",
                            inputValidation = { it.isNotBlank() && favouriteRouteStops.none { g -> it anyEquals g.name } },
                            placeholder = "名稱" withEn "Name",
                            onDismissRequest = { _, _ -> adding = false },
                            onConfirmation = {
                                adding = false
                                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                                    add(FavouriteRouteGroup(it.asBilingualText(), emptyList()))
                                }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            }
                        )
                    }
                }
            }
        }
    }
}

data class FavouriteStopItem(
    val stopId: String,
    val stop: Stop,
    val kmbCtbJointRouteNumber: String?,
    val distance: Double = Double.POSITIVE_INFINITY
)

private val stopCoIconLayers: Map<Operator, List<IconPainterLayer>> = mapOf(
    Operator.MTR to listOf(
        IconPainterLayer({ painterResource(DrawableResource("mtr_background_vector.xml")) }, Color.White) { scale(0.9F) },
        IconPainterLayer({ painterResource(DrawableResource("mtr_vector.xml")) }, Color(0xFFAC2E44))
    ),
    Operator.LRT to listOf(
        IconPainterLayer({ painterResource(DrawableResource("lrt_background_vector.xml")) }, Color.White) { scale(0.9F) },
        IconPainterLayer({ painterResource(DrawableResource("lrt_vector.xml")) }, Color(0xFFCDA410))
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteStopInterface(instance: AppActiveContext, visible: Boolean) {
    val alternateStopNameShowing by Shared.alternateStopNamesShowingState.collectAsStateWithLifecycle()
    val favouriteStopsRaw by Shared.favoriteStops.collectAsStateMultiplatform()
    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(null) }
    val favouriteStops by remember { derivedStateOf {
        favouriteStopsRaw
            .mapNotNullTo(mutableListOf()) { it.stop(instance)?.let { s -> FavouriteStopItem(it.stopId, s, it.kmbCtbJointRouteNumber) } }
            .apply {
                location?.let { l ->
                    onEachIndexed { index, item ->
                        val distance = item.stop.location.distance(l)
                        if (distance <= 0.3) {
                            this[index] = item.copy(distance = distance)
                        }
                    }
                }
                sortBy { it.distance }
            }
    } }
    val sheetState = rememberPlatformModalBottomSheetState()
    var editingStops by remember { mutableStateOf(false) }

    LaunchedEffect (visible) {
        if (visible) {
            if (location == null) {
                val result = getGPSLocation(instance, LocationPriority.FASTER).awaitWithTimeout(3000)
                if (result?.isSuccess == true && location == null) {
                    location = result.location!!
                }
            }
            while (true) {
                val result = getGPSLocation(instance).await()
                if (result?.isSuccess == true) {
                    location = result.location!!
                }
                delay(Shared.ETA_UPDATE_INTERVAL.toLong())
            }
        }
    }

    if (favouriteStops.isEmpty()) {
        EmptyBackgroundInterface(
            instance = instance,
            icon = PlatformIcons.Filled.WrongLocation,
            text = if (Shared.language == "en") "No Favourite Stops" else "沒有收藏巴士站"
        )
    } else {
        val pagerState = rememberPagerState { favouriteStops.size }
        val scope = rememberCoroutineScope()
        val routes: MutableMap<Stop, ImmutableList<StopIndexedRouteSearchResultEntry>> = remember { mutableStateMapOf() }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabScrollable = remember(favouriteStops) { mutableStateMapOf<Int, Boolean>() }
            val tabTexts = remember(favouriteStops) { favouriteStops.asSequence().map { it.stop.name[Shared.language] }.toImmutableList() }
            val renderSizes = tabTexts.renderedSizes(16F.sp, spanStyle = SpanStyle(fontWeight = FontWeight.Bold))
            AdvanceTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = pagerState.currentPage.coerceAtMost(pagerState.pageCount - 1),
                totalTabSize = pagerState.pageCount,
                scrollable = tabScrollable.values.any { it },
                widestTabWidth = renderSizes.values.maxOfOrNull { it.size.width }?.equivalentDp?.plus(20.dp)?: 1.dp
            ) { scrollable ->
                favouriteStops.forEachIndexed { index, (stopId, stop, kmbCtbJointRouteNumber, distance) ->
                    val stopName by remember(stopId, stop, kmbCtbJointRouteNumber, alternateStopNameShowing) { derivedStateOf {
                        if (kmbCtbJointRouteNumber != null && alternateStopNameShowing) {
                            Registry.getInstance(instance).findJointAlternateStop(stopId, kmbCtbJointRouteNumber).stop.name
                        } else {
                            stop.name
                        }
                    } }
                    PlatformTab(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9F)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                            ) {
                                val co = stopId.identifyStopCo().firstCo()
                                if (co != null) {
                                    val iconLayers = stopCoIconLayers[co]
                                    if (iconLayers != null) {
                                        Box {
                                            for ((icon, color, modifier) in iconLayers) {
                                                val painter = icon.invoke()
                                                Canvas(
                                                    modifier = Modifier
                                                        .size(
                                                            width = 17F.sp.dp,
                                                            height = 16F.sp.dp
                                                        )
                                                        .run(modifier),
                                                    contentDescription = co.getOperatorName(Shared.language)
                                                ) {
                                                    with(painter) {
                                                        draw(
                                                            size = Size(17F.sp.toPx(), 17F.sp.toPx()),
                                                            colorFilter = color?.let { ColorFilter.tint(it) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                PlatformText(
                                    onTextLayout = {
                                        if (!scrollable) {
                                            tabScrollable[index] = it.didOverflowWidth || it.didOverflowHeight
                                        }
                                    },
                                    fontSize = 16F.sp,
                                    lineHeight = 1.1F.em,
                                    maxLines = 1,
                                    text = stopName[Shared.language]
                                )
                                if (distance.isFinite()) {
                                    val painter = PlatformIcons.Filled.NearMe
                                    val color = LocalContentColor.current
                                    Canvas(
                                        modifier = Modifier.size(
                                            width = 17F.sp.dp,
                                            height = 16F.sp.dp
                                        ),
                                        contentDescription = if (Shared.language == "en") "Nearby" else "附近"
                                    ) {
                                        with(painter) {
                                            draw(
                                                size = Size(17F.sp.toPx(), 17F.sp.toPx()),
                                                colorFilter = ColorFilter.tint(color)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            HorizontalPager(
                modifier = Modifier.weight(1F),
                state = pagerState,
                userScrollEnabled = true
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isOnPage by remember(it, visible) { derivedStateOf { pagerState.currentPage == it && visible } }
                    val data by remember(it) { derivedStateOf { favouriteStops[it] } }
                    LaunchedEffect (data, isOnPage) {
                        val stop = data.stop
                        if (isOnPage) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val nearbyRoutes = Registry.getInstance(instance).getNearbyRoutes(stop.location, emptyMap(), false).result.toStopIndexed(instance).asImmutableList()
                                withContext(Dispatchers.Main) {
                                    routes[stop] = nearbyRoutes
                                }
                            }
                        }
                    }
                    val (stopId, stop) = data
                    val co by remember(stopId) { derivedStateOf { stopId.identifyStopCo().firstCo()!! } }
                    routes[stop]?.let { route ->
                        val extraActions: @Composable RowScope.() -> Unit = {
                            val haptic = LocalHapticFeedback.current
                            PlatformButton(
                                modifier = Modifier
                                    .width(45.dp)
                                    .fillMaxHeight()
                                    .plainTooltip(if (Shared.language == "en") "Edit Favourite Stops" else "編輯收藏巴士站"),
                                onClick = { editingStops = true },
                                contentPadding = PaddingValues(10.dp),
                                shape = platformLargeShape,
                                colors = ButtonDefaults.textButtonColors(),
                                content = {
                                    PlatformIcon(
                                        modifier = Modifier.requiredSize(23.dp),
                                        painter = PlatformIcons.Filled.EditNote,
                                        tint = when (co) {
                                            Operator.MTR -> Color.White
                                            Operator.LRT -> Color(0xFF001F50)
                                            else -> null
                                        },
                                        contentDescription = if (Shared.language == "en") "Edit Favourite Stops" else "編輯收藏巴士站"
                                    )
                                }
                            )
                            PlatformButton(
                                modifier = Modifier
                                    .width(45.dp)
                                    .fillMaxHeight()
                                    .plainTooltip(if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"),
                                onClick = instance.handleOpenMaps(stop.location, stop.name[Shared.language], false, haptic.common),
                                contentPadding = PaddingValues(10.dp),
                                shape = platformLargeShape,
                                colors = ButtonDefaults.textButtonColors(),
                                content = {
                                    PlatformIcon(
                                        modifier = Modifier.requiredSize(23.dp),
                                        painter = PlatformIcons.Filled.Map,
                                        tint = when (co) {
                                            Operator.MTR -> Color.White
                                            Operator.LRT -> Color(0xFF001F50)
                                            else -> null
                                        },
                                        contentDescription = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                                    )
                                }
                            )
                            stop.kmbBbiId?.let {
                                PlatformButton(
                                    modifier = Modifier
                                        .width(45.dp)
                                        .fillMaxHeight()
                                        .plainTooltip(if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"),
                                    onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$it.jpg", false, haptic.common),
                                    contentPadding = PaddingValues(10.dp),
                                    shape = platformLargeShape,
                                    colors = ButtonDefaults.textButtonColors(),
                                    content = {
                                        PlatformIcon(
                                            modifier = Modifier.requiredSize(23.dp),
                                            painter = PlatformIcons.Filled.TransferWithinAStation,
                                            contentDescription = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                                        )
                                    }
                                )
                            }
                        }
                        if (co.isTrain) {
                            FavouriteTrainStationInterface(
                                instance = instance,
                                stopId = stopId,
                                stop = stop,
                                co = co,
                                extraActions = extraActions
                            )
                        } else {
                            ListRoutesInterface(
                                instance = instance,
                                routes = route,
                                checkSpecialDest = true,
                                listType = RouteListType.FAVOURITE_STOP,
                                showEta = true,
                                recentSort = RecentSortMode.CHOICE,
                                proximitySortOrigin = stop.location.asOriginData(false),
                                proximitySortOriginIsRealLocation = true,
                                showEmptyText = visible,
                                visible = visible,
                                extraActions = extraActions
                            )
                        }
                    }?: run {
                        ListRoutesInterface(
                            instance = instance,
                            routes = persistentListOf(),
                            checkSpecialDest = true,
                            listType = RouteListType.FAVOURITE_STOP,
                            showEta = true,
                            recentSort = RecentSortMode.CHOICE,
                            proximitySortOrigin = stop.location.asOriginData(false),
                            proximitySortOriginIsRealLocation = true,
                            showEmptyText = false,
                            visible = visible
                        )
                    }
                }
            }
        }
    }

    if (editingStops) {
        val actionScroll = rememberScrollState()
        var deleting: String? by remember { mutableStateOf(null) }
        LaunchedEffect (deleting) {
            ScreenState.hasInterruptElement.value = deleting != null
        }
        PlatformModalBottomSheet(
            onDismissRequest = { editingStops = false },
            sheetState = sheetState
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .platformHorizontalDividerShadow(5.dp)
                            .background(platformPrimaryContainerColor)
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            text = buildAnnotatedString {
                                append(if (Shared.language == "en") "Edit Favourite Stops" else "編輯收藏巴士站")
                                appendLine()
                                append(if (Shared.language == "en") "(Nearby Stops will always be shown first)" else "(應用程式會首先排列鄰近的巴士站)", SpanStyle(fontSize = TextUnit.Small))
                            },
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                content = { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(padding)
                    ) {
                        ReorderableColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScrollWithScrollbar(
                                    state = actionScroll,
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                    scrollbarConfig = ScrollBarConfig(
                                        indicatorThickness = 4.dp
                                    )
                                ),
                            list = favouriteStopsRaw,
                            onSettle = { fromIndex, toIndex ->
                                val list = favouriteStopsRaw.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                                Registry.getInstance(instance).setFavouriteStops(list, instance)
                            }
                        ) { _, item, isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 2.dp else 0.dp)
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                key(item) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .background(platformComponentBackgroundColor)
                                            .shadow(elevation)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.Start),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1F),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val autoResizeTextState = rememberAutoResizeTextState(
                                                fontSizeRange = FontSizeRange(max = 22.sp)
                                            )
                                            AutoResizeText(
                                                textAlign = TextAlign.Start,
                                                autoResizeTextState = autoResizeTextState,
                                                text = item.stop(instance)!!.name[Shared.language]
                                            )
                                            val co = item.stopId.identifyStopCo().firstCo()
                                            if (co != null) {
                                                val iconLayers = stopCoIconLayers[co]
                                                if (iconLayers != null) {
                                                    Box {
                                                        for ((icon, color, modifier) in iconLayers) {
                                                            val painter = icon.invoke()
                                                            val fontSize = autoResizeTextState.value.fontSize
                                                            Canvas(
                                                                modifier = Modifier
                                                                    .size(
                                                                        width = fontSize.dp + 1.sp.dp,
                                                                        height = fontSize.dp
                                                                    )
                                                                    .run(modifier),
                                                                contentDescription = co.getOperatorName(Shared.language)
                                                            ) {
                                                                with(painter) {
                                                                    draw(
                                                                        size = Size(fontSize.toPx() + 1.sp.toPx(), fontSize.toPx() + 1.sp.toPx()),
                                                                        colorFilter = color?.let { ColorFilter.tint(it) }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        PlatformButton(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            colors = ButtonDefaults.clearColors(),
                                            contentPadding = PaddingValues(0.dp),
                                            onClick = { deleting = item.stopId }
                                        ) {
                                            PlatformIcon(
                                                modifier = Modifier.size(26.dp),
                                                tint = Color.Red,
                                                painter = PlatformIcons.Outlined.DeleteForever,
                                                contentDescription = if (Shared.language == "en") "Delete" else "刪除"
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .draggableHandle(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PlatformIcon(
                                                modifier = Modifier.size(26.dp),
                                                painter = PlatformIcons.Outlined.Reorder,
                                                contentDescription = if (Shared.language == "en") "Reorder" else "排序"
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            )
        }
        if (deleting != null) {
            val stopName by remember(deleting) { derivedStateOf { deleting?.asStop(instance)?.name } }
            DeleteDialog(
                icon = PlatformIcons.Filled.DeleteForever,
                title = "確認刪除巴士站\n${stopName?.zh}" withEn "Confirm Removal\n${stopName?.en}",
                text = "一經確認將不能復原" withEn "This action cannot be undone.",
                onDismissRequest = { deleting = null },
                onConfirmation = {
                    val deleteId = deleting
                    deleting = null
                    val updated = Shared.favoriteStops.value.filter { it.stopId != deleteId }
                    Registry.getInstance(instance).setFavouriteStops(updated, instance)
                }
            )
        }
    }
}

@Composable
fun FavouriteTrainStationInterface(
    instance: AppActiveContext,
    stopId: String,
    stop: Stop,
    co: Operator,
    extraActions: (@Composable RowScope.() -> Unit)? = null
) {
    val appAlert by ComposeShared.rememberAppAlert(instance)
    Column {
        ComposeShared.AnimatedVisibilityColumnAppAlert(
            context = instance,
            appAlert = appAlert
        )
        when (co) {
            Operator.MTR -> {
                MTRETADisplayInterface(
                    stopId = stopId,
                    stop = stop,
                    sheetInfoTypeState = remember { mutableStateOf(StationInfoSheetType.NONE) },
                    isPreview = true,
                    instance = instance,
                    extraActions = extraActions
                )
            }
            Operator.LRT -> {
                LRTETADisplayInterface(
                    stopId = stopId,
                    stop = stop,
                    sheetInfoTypeState = remember { mutableStateOf(StationInfoSheetType.NONE) },
                    isPreview = true,
                    instance = instance,
                    extraActions = extraActions
                )
            }
        }
    }
}