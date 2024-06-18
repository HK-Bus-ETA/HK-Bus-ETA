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

package com.loohp.hkbuseta.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.utils.DrawableResource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


@Composable
inline fun RightToLeftRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    crossinline content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                content.invoke(this)
            }
        }
    }
}

@Composable
inline fun LeftToRightLayout(
    crossinline content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        content.invoke()
    }
}

@Composable
inline fun <T> ConditionalComposable(
    condition: Boolean,
    ifTrue: @Composable (@Composable T.() -> Unit) -> Unit,
    ifFalse: @Composable (@Composable T.() -> Unit) -> Unit,
    noinline content: @Composable T.() -> Unit
) {
    if (condition) {
        ifTrue.invoke(content)
    } else {
        ifFalse.invoke(content)
    }
}

inline val WindowSizeClass.isNarrow: Boolean get() {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        WindowWidthSizeClass.Medium -> heightSizeClass == WindowHeightSizeClass.Expanded
        else -> false
    }
}

sealed class AdaptiveTopBottomMode {
    data object BottomToLeft: AdaptiveTopBottomMode()
    data object BottomToRight: AdaptiveTopBottomMode()
    data class BottomToFloating(
        val alignment: Alignment,
        val aspectRatio: Float,
        val offsetState: MutableState<Offset>
    ): AdaptiveTopBottomMode()
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveTopBottomLayout(
    modifier: Modifier = Modifier,
    context: AppContext,
    bottomSize: @Composable (WindowSizeClass) -> Dp,
    mode: AdaptiveTopBottomMode = AdaptiveTopBottomMode.BottomToLeft,
    animateSize: Boolean = false,
    top: @Composable BoxScope.(WindowSizeClass) -> Unit,
    bottom: @Composable BoxScope.(WindowSizeClass) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        var width by remember { mutableIntStateOf(0) }
        val windowSizeClass = calculateWindowSizeClass()
        LaunchedEffect (Unit) {
            while (true) {
                width = context.screenWidth
                delay(200)
            }
        }
        when {
            windowSizeClass.isNarrow -> {
                val bottomSizeValue = bottomSize.invoke(windowSizeClass)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        modifier = Modifier
                            .applyIf(animateSize) { animateContentSize() }
                            .weight(1F)
                    ) {
                        top.invoke(this, windowSizeClass)
                    }
                    Box(
                        modifier = Modifier
                            .applyIf(animateSize) { animateContentSize() }
                            .height(bottomSizeValue)
                    ) {
                        bottom.invoke(this, windowSizeClass)
                    }
                }
            }
            else -> {
                val bottomSizeValue = bottomSize.invoke(windowSizeClass)
                when (mode) {
                    is AdaptiveTopBottomMode.BottomToLeft -> {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .applyIf(animateSize) { animateContentSize() }
                                    .width(bottomSizeValue)
                            ) {
                                bottom.invoke(this, windowSizeClass)
                            }
                            Box(
                                modifier = Modifier
                                    .applyIf(animateSize) { animateContentSize() }
                                    .weight(1F)
                            ) {
                                top.invoke(this, windowSizeClass)
                            }
                        }
                    }
                    is AdaptiveTopBottomMode.BottomToRight -> {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .applyIf(animateSize) { animateContentSize() }
                                    .weight(1F)
                            ) {
                                top.invoke(this, windowSizeClass)
                            }
                            Box(
                                modifier = Modifier
                                    .applyIf(animateSize) { animateContentSize() }
                                    .width(bottomSizeValue)
                            ) {
                                bottom.invoke(this, windowSizeClass)
                            }
                        }
                    }
                    is AdaptiveTopBottomMode.BottomToFloating -> {
                        var offset by mode.offsetState
                        var maxSize by remember { mutableStateOf(IntSize.Zero) }
                        var size by remember { mutableStateOf(IntSize.Zero) }
                        val updateOffset: (Offset) -> Unit = remember { { offsetChange ->
                            var (x, y) = offset + offsetChange
                            x = x.coerceIn(0F - size.width / 4F * 3F, maxSize.width - size.width / 4F)
                            y = y.coerceIn(-(maxSize.height - size.height).toFloat(), 0F + size.height / 4F * 3F)
                            offset = Offset(x, y)
                        } }
                        val transformableState = rememberTransformableState { _, offsetChange, _ -> updateOffset.invoke(offsetChange) }
                        LaunchedEffect (width) {
                            updateOffset.invoke(Offset.Zero)
                        }
                        Box(
                            modifier = Modifier.onSizeChanged { maxSize = it },
                            contentAlignment = mode.alignment
                        ) {
                            top.invoke(this, windowSizeClass)
                            Column(
                                modifier = Modifier
                                    .size(bottomSizeValue, bottomSizeValue * (1F / mode.aspectRatio))
                                    .onSizeChanged { size = it }
                                    .graphicsLayer {
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .platformHorizontalDividerShadow(5.dp)
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .background(Color.Gray)
                                        .transformable(transformableState),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.DarkGray,
                                        imageVector = Icons.Outlined.Menu,
                                        contentDescription = if (Shared.language == "en") "Drag Handle" else "拖曳手柄"
                                    )
                                }
                                Box {
                                    bottom.invoke(this, windowSizeClass)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AdaptiveNavBarItem(
    val label: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val selected: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalResourceApi::class)
@Composable
fun AdaptiveNavBar(
    context: AppActiveContext,
    content: @Composable () -> Unit,
    itemCount: Int,
    items: @Composable (Int) -> AdaptiveNavBarItem
) {
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val windowSizeClass = calculateWindowSizeClass()
    LaunchedEffect (Unit) {
        while (true) {
            width = context.screenWidth
            height = context.screenHeight
            delay(200)
        }
    }
    when {
        windowSizeClass.isNarrow -> {
            Scaffold(
                content = {
                    Box(
                        modifier = Modifier.padding(it)
                    ) {
                        content.invoke()
                    }
                },
                bottomBar = {
                    PlatformNavigationBar {
                        (0 until itemCount).forEach { index ->
                            val item = items.invoke(index)
                            PlatformNavigationBarItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                label = item.label,
                                icon =  item.icon
                            )
                        }
                    }
                }
            )
        }
        else -> {
            Row {
                NavigationRail(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .platformVerticalDividerShadow(10.dp)
                        .zIndex(10F),
                    header = {
                        if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
                            Image(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .applyIfNotNull(platformShowDownloadAppBottomSheet) { clickable(onClick = it) },
                                painter = painterResource(DrawableResource("icon.png")),
                                contentDescription = "HK Bus ETA"
                            )
                        }
                    },
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = if (windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact) {
                                Arrangement.spacedBy(12.dp, Alignment.Bottom)
                            } else {
                                Arrangement.SpaceEvenly
                            }
                        ) {
                            (0 until itemCount).forEach { index ->
                                val item = items.invoke(index)
                                NavigationRailItem(
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                    selected = item.selected,
                                    onClick = item.onClick,
                                    label = item.label,
                                    icon =  item.icon
                                )
                            }
                        }
                    },
                )
                content.invoke()
            }
        }
    }
}