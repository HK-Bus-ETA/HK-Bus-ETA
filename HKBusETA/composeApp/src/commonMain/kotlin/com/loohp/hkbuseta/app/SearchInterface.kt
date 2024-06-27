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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.getPossibleNextChar
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.compose.AdaptiveTopBottomLayout
import com.loohp.hkbuseta.compose.AdaptiveTopBottomMode
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.Backspace
import com.loohp.hkbuseta.compose.Delete
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformFloatingActionButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.isNarrow
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.compose.platformExtraLargeShape
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.rememberAutoResizeFontState
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.keyString
import com.loohp.hkbuseta.utils.pixelsToDp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.min


@Immutable
data class RouteKeyboardState(
    val text: String,
    val nextCharResult: Registry.PossibleNextCharResult,
    val categories: Set<OperatorCategory>,
    val routes: ImmutableList<StopIndexedRouteSearchResultEntry>
)

enum class OperatorCategory(
    val operators: Set<Operator>,
    val displayName: (String) -> String
) {
    BUS(
        operators = setOf(Operator.KMB, Operator.CTB, Operator.NLB, Operator.MTR_BUS),
        displayName = { if (it == "en") "Bus" else "巴士" }
    ),
    GMB(
        operators = setOf(Operator.GMB),
        displayName = { if (it == "en") "GMB" else "小巴" }
    ),
    LRT(
        operators =  setOf(Operator.LRT),
        displayName = { Operator.LRT.getDisplayName("", false, it) }
    ),
    MTR(
        operators = setOf(Operator.MTR),
        displayName = { Operator.MTR.getDisplayName("", false, it) }
    ),
    FERRY(
        operators = setOf(Operator.SUNFERRY, Operator.HKKF, Operator.FORTUNEFERRY),
        displayName = { if (it == "en") "Ferry" else "渡輪" }
    )
}

fun Set<OperatorCategory>.contains(operator: Operator): Boolean {
    return any { it.operators.contains(operator) }
}

fun Set<OperatorCategory>.containsAll(operator: Collection<Operator>): Boolean {
    val copy = operator.toMutableList()
    forEach { copy.removeAll(it.operators) }
    return copy.isEmpty()
}

fun Set<OperatorCategory>.toggle(operatorCategory: OperatorCategory): Set<OperatorCategory> {
    return if (containsAll(OperatorCategory.entries)) {
        setOf(operatorCategory)
    } else {
        val copy = toMutableSet()
        if (!copy.remove(operatorCategory)) {
            copy.add(operatorCategory)
        }
        return copy.ifEmpty { OperatorCategory.entries.toSet() }
    }
}

private val searchState: MutableStateFlow<RouteKeyboardState> = MutableStateFlow(RouteKeyboardState("", Registry.PossibleNextCharResult(emptySet(), false), OperatorCategory.entries.toSet(), persistentListOf()))

private suspend fun updateSearchState(text: String = searchState.value.text, categories: Set<OperatorCategory> = searchState.value.categories, resultProvider: (suspend (String, Set<OperatorCategory>) -> List<StopIndexedRouteSearchResultEntry>)? = null) {
    val result = resultProvider?.invoke(text, categories)?: searchState.value.routes
    val possibleNextChar = result.getPossibleNextChar(text)
    searchState.value = RouteKeyboardState(text, possibleNextChar, categories, result.toImmutableList())
}

private val floatingKeyboardState: MutableStateFlow<Offset> = MutableStateFlow(Offset.Zero)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInterface(instance: AppActiveContext, visible: Boolean) {
    val scope = rememberCoroutineScope()

    val listType = instance.compose.data["listType"] as? RouteListType ?: RouteListType.NORMAL
    val showEta = instance.compose.data["showEta"] as? Boolean?: false
    val recentSort = instance.compose.data["recentSort"] as? RecentSortMode ?: RecentSortMode.DISABLED
    val proximitySortOrigin = instance.compose.data["proximitySortOrigin"] as? Coordinates

    var showEmptyText by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf(Shared.language) }
    val state by searchState.collectAsStateMultiplatform()
    val keyPressFocus = remember { FocusRequester() }
    var size by remember { mutableStateOf(IntSize(instance.screenWidth, instance.screenHeight)) }

    val keyboardOffsetState = floatingKeyboardState.collectAsStateMultiplatform()

    LaunchedEffect (visible, state.categories) {
        if (visible) {
            updateSearchState { t, c -> Registry.getInstance(instance).findRoutes(t, false) { r -> c.containsAll(r.co) }.toStopIndexed(instance) }
            keyPressFocus.requestFocus()
        }
        language = Shared.language
    }
    LaunchedEffect (state) {
        if (visible) {
            keyPressFocus.requestFocus()
        }
    }
    LaunchedEffect (state) {
        showEmptyText = state.text.isNotEmpty() || state.categories.isEmpty()
    }
    LaunchedEffect (visible) {
        if (visible) {
            keyPressFocus.requestFocus()
        }
    }

    AdaptiveTopBottomLayout(
        modifier = Modifier.onSizeChanged { size = it },
        context = instance,
        mode = AdaptiveTopBottomMode.BottomToFloating(
            alignment = Alignment.BottomStart,
            aspectRatio = 4F / 3F,
            offsetState = keyboardOffsetState
        ),
        bottomSize = {
            if (it.isNarrow) {
                (size.height / 11F * 4F).pixelsToDp(instance).dp.coerceIn(178.dp, 248.dp)
            } else {
                min(412F, (size.width / 2F).pixelsToDp(instance)).dp
            }
        },
        top = {  _ ->
            Column {
                Column(
                    modifier = Modifier
                        .zIndex(1F)
                        .platformHorizontalDividerShadow(5.dp)
                        .background(platformBackgroundColor)
                        .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .clip(platformExtraLargeShape)
                            .background(platformPrimaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformText(
                            modifier = Modifier
                                .onKeyEvent {
                                    if (it.type == KeyEventType.KeyDown) {
                                        val key = it.key
                                        return@onKeyEvent when {
                                            key == Key.Backspace || (key == Key.Back && composePlatform.isMobileAppRunningOnDesktop && composePlatform.applePlatform) -> {
                                                CoroutineScope(dispatcherIO).launch {
                                                    handleInput(instance, '<')
                                                }
                                                true
                                            }
                                            key == Key.Delete -> {
                                                CoroutineScope(dispatcherIO).launch {
                                                    handleInput(instance, '-')
                                                }
                                                true
                                            }
                                            key.keyString?.let { s -> state.nextCharResult.characters.contains(s[0]) } == true -> {
                                                CoroutineScope(dispatcherIO).launch {
                                                    handleInput(instance, key.keyString!![0])
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    false
                                }
                                .focusRequester(keyPressFocus)
                                .focusable(),
                            textAlign = TextAlign.Center,
                            fontSize = 30.sp,
                            maxLines = 1,
                            text = Shared.getMtrLineName(state.text).takeUnless { it.isEmpty() }?: if (Shared.language == "en") "Input Route" else "輸入路線"
                        )
                    }
                    Row(
                        modifier = Modifier.height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val autoFontSizeState = rememberAutoResizeFontState(fontSizeRange = FontSizeRange(max = 17F.sp))
                        for (category in OperatorCategory.entries) {
                            PlatformButton(
                                onClick = {
                                    CoroutineScope(dispatcherIO).launch {
                                        updateSearchState(categories = state.categories.toggle(category))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1F),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = platformPrimaryContainerColor
                                        .adjustAlpha(if (state.categories.contains(category)) 1F else 0.2F)
                                ),
                                shape = platformLargeShape,
                                contentPadding = PaddingValues(0.dp),
                                content = {
                                    AutoResizeText(
                                        autoResizeTextState = autoFontSizeState,
                                        lineHeight = 1.1F.em,
                                        maxLines = 1,
                                        text = category.displayName.invoke(language)
                                    )
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val hasTrains by remember(state.routes) { derivedStateOf { state.routes.isEmpty() || state.routes.any { it.co.isTrain } } }
                    val trainButtonOffset by animateDpAsState(
                        targetValue = if (hasTrains) 0.dp else 100.dp,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                    ListRoutesInterface(instance, state.routes, listType, showEta, recentSort, proximitySortOrigin, showEmptyText, visible, false)
                    if (trainButtonOffset < 100.dp) {
                        PlatformFloatingActionButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .graphicsLayer { translationX = trainButtonOffset.toPx() }
                                .plainTooltip(if (Shared.language == "en") "MTR System Map & LRT Route Map" else "港鐵及輕鐵路綫圖"),
                            onClick = {
                                instance.startActivity(AppIntent(instance, AppScreen.SEARCH_TRAIN))
                            },
                        ) {
                            Image(
                                modifier = Modifier.size(27.dp),
                                painter = painterResource(DrawableResource("mtr.png")),
                                contentDescription = if (Shared.language == "en") "MTR System Map & LRT Route Map" else "港鐵及輕鐵路綫圖"
                            )
                        }
                    }
                }
            }
        },
        bottom = { _ ->
            val spacing = 8.dp
            val onClick: () -> Unit = { scope.launch { keyPressFocus.requestFocus() } }
            var keyHeight by remember { mutableIntStateOf(0) }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { /* do nothing */ }
                    .pointerHoverIcon(PointerIcon.Default)
            )
            Column(
                modifier = Modifier
                    .platformHorizontalDividerShadow(5.dp)
                    .fillMaxSize()
                    .background(if (Shared.theme.isDarkMode) Color(0xff111111) else Color(0xFFEAEAEA))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1F),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Column(
                        modifier = Modifier.weight(1F),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Box(modifier = Modifier.weight(1F).onSizeChanged { keyHeight = it.height }) { KeyboardButton(instance, '1', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '4', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '7', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '-', Color.Red, persistentListOf(PlatformIcons.Outlined.Delete.asImmutableState()), onClick) }
                    }
                    Column(
                        modifier = Modifier.weight(1F),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '2', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '5', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '8', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '0', onClick) }
                    }
                    Column(
                        modifier = Modifier.weight(1F),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '3', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '6', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '9', onClick) }
                        Box(modifier = Modifier.weight(1F)) { KeyboardButton(instance, '<', Color.Red, persistentListOf(PlatformIcons.AutoMirrored.Outlined.Backspace.asImmutableState()), onClick) }
                    }
                    Box (
                        modifier = Modifier.weight(1.2F),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val scroll = rememberScrollState()
                        val possibleValues by remember { derivedStateOf { state.nextCharResult.characters } }

                        Column (
                            modifier = Modifier
                                .verticalScrollWithScrollbar(
                                    state = scroll,
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                    scrollbarConfig = ScrollBarConfig(
                                        indicatorThickness = 4.dp
                                    )
                                )
                                .padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(spacing - 3.dp)
                        ) {
                            for (alphabet in 'A'..'Z') {
                                if (possibleValues.contains(alphabet)) {
                                    Box(modifier = Modifier.heightIn(max = keyHeight.equivalentDp * 0.95F)) {
                                        KeyboardButton(instance, alphabet, onClick)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

fun handleInput(instance: AppActiveContext, input: Char) {
    val originalText = searchState.value.text
    val categories = searchState.value.categories
    val (newText, newCategories) = if (input == '<') {
        if (originalText.isNotEmpty()) {
            originalText.dropLast(1)
        } else {
            originalText
        } to categories
    } else if (input == '-') {
        if (originalText.isEmpty()) {
            "" to OperatorCategory.entries.toSet()
        } else {
            "" to categories
        }
    } else {
        (originalText + input) to categories
    }
    val result = Registry.getInstance(instance).findRoutes(newText, false) { r -> newCategories.containsAll(r.co) }.toStopIndexed(instance)
    val possibleNextChar = result.getPossibleNextChar(newText)
    searchState.value = RouteKeyboardState(newText, possibleNextChar, newCategories, result.toImmutableList())
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, onClick: () -> Unit = { /* do nothing */ }) {
    KeyboardButton(instance, content, Color.Unspecified, persistentListOf(), onClick)
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, color: Color, icons: ImmutableList<ImmutableState<Any>>, onClick: () -> Unit = { /* do nothing */ }) {
    val state by searchState.collectAsStateMultiplatform()
    val icon = if (icons.isEmpty()) null else icons[0].value
    val enabled = when (content) {
        '<', '-' -> true
        else -> state.nextCharResult.characters.contains(content)
    }
    val actualColor = color.takeOrElse { platformLocalContentColor }.adjustAlpha(if (enabled) 1F else 0.25F)

    PlatformButton(
        onClick = {
            CoroutineScope(dispatcherIO).launch {
                handleInput(instance, content)
                onClick.invoke()
            }
        },
        modifier = Modifier
            .applyIf(content.isLetter() || content == '!') { heightIn(max = 47.5F.dp) }
            .fillMaxSize(),
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (Shared.theme.isDarkMode) Color.DarkGray else Color.White),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(0.dp),
        content = {
            when (icon) {
                null -> {
                    PlatformText(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 26.sp,
                        color = actualColor,
                        text = content.toString()
                    )
                }
                is Painter -> {
                    PlatformIcon(
                        modifier = Modifier.requiredSize(26.dp),
                        painter = icon,
                        contentDescription = content.toString(),
                        tint = actualColor,
                    )
                }
                is String -> {
                    Image(
                        modifier = Modifier.requiredSize(26.dp),
                        painter = painterResource(DrawableResource(icon)),
                        contentDescription = content.toString()
                    )
                }
            }
        }
    )
}