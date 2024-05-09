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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CaretProperties
import androidx.compose.material3.CaretScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp


object PlatformIcons {
    object Filled
    object Outlined
    object AutoMirrored {
        object Filled
        object Outlined
        val Default = Filled
    }
    val Default = Filled
}

expect val PlatformIcons.AutoMirrored.Filled.ArrowBack: Painter @Composable get
expect val PlatformIcons.AutoMirrored.Filled.OpenInNew: Painter @Composable get
expect val PlatformIcons.AutoMirrored.Filled.Sort: Painter @Composable get
expect val PlatformIcons.AutoMirrored.Outlined.Accessible: Painter @Composable get
expect val PlatformIcons.AutoMirrored.Outlined.Backspace: Painter @Composable get
expect val PlatformIcons.AutoMirrored.Outlined.AltRoute: Painter @Composable get
expect val PlatformIcons.Filled.ArrowDownward: Painter @Composable get
expect val PlatformIcons.Filled.ArrowUpward: Painter @Composable get
expect val PlatformIcons.Filled.ChevronLeft: Painter @Composable get
expect val PlatformIcons.Filled.ChevronRight: Painter @Composable get
expect val PlatformIcons.Filled.FirstPage: Painter @Composable get
expect val PlatformIcons.Filled.LastPage: Painter @Composable get
expect val PlatformIcons.Filled.ArrowDropDown: Painter @Composable get
expect val PlatformIcons.Filled.Close: Painter @Composable get
expect val PlatformIcons.Filled.Dangerous: Painter @Composable get
expect val PlatformIcons.Filled.DarkMode: Painter @Composable get
expect val PlatformIcons.Filled.DeleteForever: Painter @Composable get
expect val PlatformIcons.Filled.EditNote: Painter @Composable get
expect val PlatformIcons.Filled.Forest: Painter @Composable get
expect val PlatformIcons.Filled.History: Painter @Composable get
expect val PlatformIcons.Filled.Info: Painter @Composable get
expect val PlatformIcons.Filled.Translate: Painter @Composable get
expect val PlatformIcons.Filled.LocationDisabled: Painter @Composable get
expect val PlatformIcons.Filled.Map: Painter @Composable get
expect val PlatformIcons.Filled.NearMe: Painter @Composable get
expect val PlatformIcons.Filled.NearMeDisabled: Painter @Composable get
expect val PlatformIcons.Filled.NoTransfer: Painter @Composable get
expect val PlatformIcons.Filled.PinDrop: Painter @Composable get
expect val PlatformIcons.Filled.PriorityHigh: Painter @Composable get
expect val PlatformIcons.Filled.Search: Painter @Composable get
expect val PlatformIcons.Filled.Settings: Painter @Composable get
expect val PlatformIcons.Filled.Star: Painter @Composable get
expect val PlatformIcons.Filled.Sync: Painter @Composable get
expect val PlatformIcons.Filled.TransferWithinAStation: Painter @Composable get
expect val PlatformIcons.Filled.Update: Painter @Composable get
expect val PlatformIcons.Filled.WrongLocation: Painter @Composable get
expect val PlatformIcons.Outlined.Add: Painter @Composable get
expect val PlatformIcons.Outlined.Bedtime: Painter @Composable get
expect val PlatformIcons.Outlined.Delete: Painter @Composable get
expect val PlatformIcons.Outlined.DeleteForever: Painter @Composable get
expect val PlatformIcons.Outlined.Download: Painter @Composable get
expect val PlatformIcons.Outlined.Edit: Painter @Composable get
expect val PlatformIcons.Outlined.EmojiFlags: Painter @Composable get
expect val PlatformIcons.Outlined.Fingerprint: Painter @Composable get
expect val PlatformIcons.Outlined.Fullscreen: Painter @Composable get
expect val PlatformIcons.Outlined.FullscreenExit: Painter @Composable get
expect val PlatformIcons.Outlined.History: Painter @Composable get
expect val PlatformIcons.Outlined.LightMode: Painter @Composable get
expect val PlatformIcons.Outlined.LocationOff: Painter @Composable get
expect val PlatformIcons.Outlined.LocationOn: Painter @Composable get
expect val PlatformIcons.Outlined.Map: Painter @Composable get
expect val PlatformIcons.Outlined.MoreHoriz: Painter @Composable get
expect val PlatformIcons.Outlined.MoreVert: Painter @Composable get
expect val PlatformIcons.Outlined.MyLocation: Painter @Composable get
expect val PlatformIcons.Outlined.NearMe: Painter @Composable get
expect val PlatformIcons.Outlined.NotificationsActive: Painter @Composable get
expect val PlatformIcons.Outlined.NotificationsOff: Painter @Composable get
expect val PlatformIcons.Outlined.Paid: Painter @Composable get
expect val PlatformIcons.Outlined.PinDrop: Painter @Composable get
expect val PlatformIcons.Outlined.Reorder: Painter @Composable get
expect val PlatformIcons.Outlined.Schedule: Painter @Composable get
expect val PlatformIcons.Outlined.Search: Painter @Composable get
expect val PlatformIcons.Outlined.Settings: Painter @Composable get
expect val PlatformIcons.Outlined.Share: Painter @Composable get
expect val PlatformIcons.Outlined.Smartphone: Painter @Composable get
expect val PlatformIcons.Outlined.Star: Painter @Composable get
expect val PlatformIcons.Outlined.StarOutline: Painter @Composable get
expect val PlatformIcons.Outlined.Start: Painter @Composable get
expect val PlatformIcons.Outlined.Streetview: Painter @Composable get
expect val PlatformIcons.Outlined.SwipeRightAlt: Painter @Composable get
expect val PlatformIcons.Outlined.SyncAlt: Painter @Composable get
expect val PlatformIcons.Outlined.TextRotationNone: Painter @Composable get
expect val PlatformIcons.Outlined.Timer: Painter @Composable get
expect val PlatformIcons.Outlined.Train: Painter @Composable get
expect val PlatformIcons.Outlined.TransferWithinAStation: Painter @Composable get
expect val PlatformIcons.Outlined.UTurnRight: Painter @Composable get
expect val PlatformIcons.Outlined.Update: Painter @Composable get
expect val PlatformIcons.Outlined.Upload: Painter @Composable get
expect val PlatformIcons.Outlined.Watch: Painter @Composable get
expect val PlatformIcons.Outlined.LineEndCircle: Painter @Composable get
expect val PlatformIcons.Outlined.MobileFriendly: Painter @Composable get

expect val Painter.shouldBeTintedForIcons: Boolean

@Composable
expect fun PlatformButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
)

@Composable
expect fun PlatformFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
)

@Composable
expect fun PlatformScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
)

@Composable
expect fun PlatformNavigationBar(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
)

@Composable
expect fun RowScope.PlatformNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
)

@Composable
expect fun PlatformText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = platformLocalTextStyle
)

@Composable
expect fun PlatformText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = platformLocalTextStyle
)

@Composable
expect fun PlatformIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
)

@Composable
expect fun PlatformIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun PlatformTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    iosDivider: (@Composable () -> Unit)? = null
)

expect val platformComponentBackgroundColor: Color @Composable get
expect val platformBackgroundColor: Color @Composable get
expect val platformPrimaryContainerColor: Color @Composable get
expect val platformSurfaceColor: Color @Composable get
expect val platformSurfaceContainerColor: Color @Composable get
expect val platformLocalContentColor: Color @Composable get
expect val platformTopBarColor: Color @Composable get
expect val platformLocalTextStyle: TextStyle @Composable get
expect val platformExtraLargeShape: Shape @Composable get
expect val platformLargeShape: Shape @Composable get

@Composable
expect fun PlatformTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    iosDivider: (@Composable () -> Unit)? = null,
    tabs: @Composable () -> Unit
)

@Composable
expect fun PlatformScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    totalTabSize: Int,
    widestTabWidth: Dp,
    tabs: @Composable () -> Unit
)

@Composable
expect fun PlatformTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun PlatformModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberPlatformModalBottomSheetState(),
    sheetMaxWidth: Dp? = null,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    desktopCloseColor: Color? = null,
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties(),
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
expect fun PlatformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
)

enum class DismissRequestType {
    BUTTON, CLICK_OUTSIDE
}

@Composable
expect fun PlatformAlertDialog(
    iosSheetStyle: Boolean = false,
    iosCloseButton: Boolean = false,
    onDismissRequest: (DismissRequestType) -> Unit,
    confirmButton: @Composable () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
)

expect fun Modifier.platformHorizontalDividerShadow(elevation: Dp = 5.dp): Modifier
expect fun Modifier.platformVerticalDividerShadow(elevation: Dp = 5.dp): Modifier

@Composable
expect fun PlatformDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
)

@Composable
expect fun PlatformDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)

@Composable
expect fun PlatformSwitch(
    checked : Boolean,
    onCheckedChange : (Boolean) -> Unit,
    modifier : Modifier = Modifier,
    thumbContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
)

@Composable
expect fun PlatformFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
)

@Composable
expect fun PlatformFilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
)

@Composable
expect fun PlatformOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
)

@Composable
expect fun PlatformLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color? = null,
    trackColor: Color? = null,
    strokeCap: StrokeCap? = null
)

@Composable
expect fun PlatformLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    trackColor: Color? = null,
    strokeCap: StrokeCap? = null
)

@Composable
expect fun PlatformCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color? = null,
    strokeWidth: Dp? = null,
    trackColor: Color? = null,
    strokeCap: StrokeCap? = null
)

expect val platformShowDownloadAppBottomSheet: (() -> Unit)?

@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun CaretScope.PlatformPlainTooltip(
    modifier: Modifier = Modifier,
    caretProperties: CaretProperties? = null,
    shape: Shape? = null,
    contentColor: Color? = null,
    containerColor: Color? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
)