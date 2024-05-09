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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.isTopOfStack
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.compose.AdaptiveNavBar
import com.loohp.hkbuseta.compose.AdaptiveNavBarItem
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.History
import com.loohp.hkbuseta.compose.NearMe
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.Search
import com.loohp.hkbuseta.compose.Settings
import com.loohp.hkbuseta.compose.Star
import com.loohp.hkbuseta.compose.StarOutline
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.platformSurfaceContainerColor
import com.loohp.hkbuseta.compose.rememberAutoResizeFontState
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch


@Immutable
data class TitleTabItem(
    val title: BilingualText,
    val unselectedIcon: @Composable () -> Painter,
    val selectedIcon: @Composable () -> Painter,
    val appScreens: Set<AppScreen>
) {

    fun icon(selected: Boolean): @Composable () -> Painter {
        return if (selected) selectedIcon else unselectedIcon
    }

}

val titleTabItems = listOf(
    TitleTabItem(
        title = "附近路線" withEn "Nearby",
        unselectedIcon = { PlatformIcons.Outlined.NearMe },
        selectedIcon = { PlatformIcons.Filled.NearMe },
        appScreens = setOf(AppScreen.NEARBY, AppScreen.TITLE)
    ),
    TitleTabItem(
        title = "喜愛" withEn "Favourites",
        unselectedIcon = { PlatformIcons.Outlined.StarOutline },
        selectedIcon = { PlatformIcons.Filled.Star },
        appScreens = setOf(AppScreen.FAV, AppScreen.FAV_ROUTE_LIST_VIEW)
    ),
    TitleTabItem(
        title = "搜尋路線" withEn "Search",
        unselectedIcon = { PlatformIcons.Outlined.Search },
        selectedIcon = { PlatformIcons.Filled.Search },
        appScreens = setOf(AppScreen.SEARCH, AppScreen.LIST_ROUTES)
    ),
    TitleTabItem(
        title = "最近瀏覽" withEn "Recents",
        unselectedIcon = { PlatformIcons.Outlined.History },
        selectedIcon = { PlatformIcons.Filled.History },
        appScreens = setOf(AppScreen.RECENT)
    ),
    TitleTabItem(
        title = "設定" withEn "Settings",
        unselectedIcon = { PlatformIcons.Outlined.Settings },
        selectedIcon = { PlatformIcons.Filled.Settings },
        appScreens = setOf(AppScreen.SETTINGS)
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TitleInterface(instance: AppActiveContext) {
    val pagerState = rememberPagerState(
        initialPage = titleTabItems.indexOfFirst { it.appScreens.contains(instance.compose.screen) }.takeIf { it >= 0 }?: 0,
        pageCount = { titleTabItems.size }
    )
    val scope = rememberCoroutineScope()

    val routes by remember(instance) { derivedStateOf { instance.compose.data["result"] as? List<*> } }
    var showBottomRouteSheet by rememberSaveable { mutableStateOf(false) }

    instance.compose.setStatusNavBarColor(
        status = platformPrimaryContainerColor,
        nav = platformSurfaceContainerColor
    )

    LaunchedEffect (routes, instance) {
        if (instance.compose.screen == AppScreen.LIST_ROUTES && routes?.let { it.size > 1 && it.any { r -> r is RouteSearchResultEntry } } == true) {
            showBottomRouteSheet = true
        }
    }
    LaunchedEffect (instance) {
        titleTabItems.indexOfFirst {
            it.appScreens.contains(instance.compose.screen)
        }.takeIf {
            it >= 0
        }?.let {
            if (instance.compose.flags.contains(AppIntentFlag.NO_ANIMATION)) {
                pagerState.scrollToPage(it)
            } else {
                pagerState.animateScrollToPage(it)
            }
        }
    }
    LaunchedEffect (pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && instance.isTopOfStack()) {
            instance.compose.switchActivity(AppIntent(instance, titleTabItems[pagerState.currentPage].appScreens.first()))
        }
    }

    val autoFontSizeState = rememberAutoResizeFontState(
        fontSizeRange = FontSizeRange(min = 9F.sp, max = 17F.sp),
        preferSingleLine = true
    )
    AdaptiveNavBar(
        context = instance,
        itemCount = titleTabItems.size,
        items = { index ->
            val item = titleTabItems[index]
            AdaptiveNavBarItem(
                selected = index == pagerState.currentPage,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                label = {
                    AutoResizeText(
                        autoResizeTextState = autoFontSizeState,
                        lineHeight = 1.1F.em,
                        textAlign = TextAlign.Center,
                        text = item.title[Shared.language]
                    )
                },
                icon =  {
                    val scale by animateFloatAsState(
                        targetValue = if (index == pagerState.currentPage) 1.1F else 1F,
                        animationSpec = tween(200, easing = LinearEasing)
                    )
                    PlatformIcon(
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scale),
                        painter = item.icon(index == pagerState.currentPage).invoke(),
                        contentDescription = item.title[Shared.language]
                    )
                }
            )
        },
        content = {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize(),
                state = pagerState,
                userScrollEnabled = true
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isOnPage = pagerState.currentPage == it && titleTabItems[it].appScreens.contains(instance.compose.screen)
                    when (it) {
                        0 -> NearbyInterface(instance, isOnPage)
                        1 -> FavouriteInterface(instance, isOnPage)
                        2 -> SearchInterface(instance, isOnPage)
                        3 -> RecentInterface(instance, isOnPage)
                        4 -> SettingsInterface(instance)
                        else -> PlatformText(titleTabItems[it].title[Shared.language])
                    }
                }
            }
        }
    )
    if (showBottomRouteSheet) {
        val sheetState = rememberPlatformModalBottomSheetState()
        PlatformModalBottomSheet(
            onDismissRequest = { showBottomRouteSheet = false },
            sheetState = sheetState
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(platformPrimaryContainerColor)
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (Shared.language == "en") "Routes" else "路線",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                content = { padding ->
                    Box(
                        modifier = Modifier.padding(padding)
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        ListRoutesInterface(instance, (routes as List<RouteSearchResultEntry>).toStopIndexed(instance).toImmutableList(), RouteListType.NORMAL, false, RecentSortMode.DISABLED, null)
                    }
                }
            )
        }
    }
}