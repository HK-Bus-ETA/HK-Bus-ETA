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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NearMeDisabled
import androidx.compose.material.icons.filled.NoTransfer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DepartureBoard
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DoubleArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiFlags
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MobileFriendly
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Start
import androidx.compose.material.icons.outlined.Streetview
import androidx.compose.material.icons.outlined.SwipeRightAlt
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.TextRotationNone
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.UTurnRight
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.isAppleDevice
import com.loohp.hkbuseta.isWasmSupported
import com.loohp.hkbuseta.setDownloadAppSheetVisible
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.equivalentDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

actual inline val PlatformIcons.AutoMirrored.Filled.ArrowBack: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack)
actual inline val PlatformIcons.AutoMirrored.Filled.OpenInNew: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Filled.OpenInNew)
actual inline val PlatformIcons.AutoMirrored.Filled.Sort: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Filled.Sort)
actual inline val PlatformIcons.AutoMirrored.Outlined.Accessible: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Outlined.Accessible)
actual inline val PlatformIcons.AutoMirrored.Outlined.Backspace: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Outlined.Backspace)
actual inline val PlatformIcons.AutoMirrored.Outlined.AltRoute: Painter @Composable get() = rememberVectorPainter(Icons.AutoMirrored.Outlined.AltRoute)
actual inline val PlatformIcons.Filled.ArrowDownward: Painter @Composable get() = rememberVectorPainter(Icons.Filled.ArrowDownward)
actual inline val PlatformIcons.Filled.ArrowUpward: Painter @Composable get() = rememberVectorPainter(Icons.Filled.ArrowUpward)
actual inline val PlatformIcons.Filled.Bolt: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Bolt)
actual inline val PlatformIcons.Filled.CheckCircle: Painter @Composable get() = rememberVectorPainter(Icons.Filled.CheckCircle)
actual inline val PlatformIcons.Filled.ChevronLeft: Painter @Composable get() = rememberVectorPainter(Icons.Filled.ChevronLeft)
actual inline val PlatformIcons.Filled.ChevronRight: Painter @Composable get() = rememberVectorPainter(Icons.Filled.ChevronRight)
actual inline val PlatformIcons.Filled.FirstPage: Painter @Composable get() = rememberVectorPainter(Icons.Filled.FirstPage)
actual inline val PlatformIcons.Filled.LastPage: Painter @Composable get() = rememberVectorPainter(Icons.Filled.LastPage)
actual inline val PlatformIcons.Filled.ArrowDropDown: Painter @Composable get() = rememberVectorPainter(Icons.Filled.ArrowDropDown)
actual inline val PlatformIcons.Filled.Close: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Close)
actual inline val PlatformIcons.Filled.Dangerous: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Dangerous)
actual inline val PlatformIcons.Filled.DarkMode: Painter @Composable get() = rememberVectorPainter(Icons.Filled.DarkMode)
actual inline val PlatformIcons.Filled.DeleteForever: Painter @Composable get() = rememberVectorPainter(Icons.Filled.DeleteForever)
actual inline val PlatformIcons.Filled.EditNote: Painter @Composable get() = rememberVectorPainter(Icons.Filled.EditNote)
actual inline val PlatformIcons.Filled.Error: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Error)
actual inline val PlatformIcons.Filled.Forest: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Forest)
actual inline val PlatformIcons.Filled.History: Painter @Composable get() = rememberVectorPainter(Icons.Filled.History)
actual inline val PlatformIcons.Filled.Info: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Info)
actual inline val PlatformIcons.Filled.Translate: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Translate)
actual inline val PlatformIcons.Filled.LocationDisabled: Painter @Composable get() = rememberVectorPainter(Icons.Filled.LocationDisabled)
actual inline val PlatformIcons.Filled.Map: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Map)
actual inline val PlatformIcons.Filled.NearMe: Painter @Composable get() = rememberVectorPainter(Icons.Filled.NearMe)
actual inline val PlatformIcons.Filled.NearMeDisabled: Painter @Composable get() = rememberVectorPainter(Icons.Filled.NearMeDisabled)
actual inline val PlatformIcons.Filled.NoTransfer: Painter @Composable get() = rememberVectorPainter(Icons.Filled.NoTransfer)
actual inline val PlatformIcons.Filled.PinDrop: Painter @Composable get() = rememberVectorPainter(Icons.Filled.PinDrop)
actual inline val PlatformIcons.Filled.PhotoCamera: Painter @Composable get() = rememberVectorPainter(Icons.Filled.PhotoCamera)
actual inline val PlatformIcons.Filled.PriorityHigh: Painter @Composable get() = rememberVectorPainter(Icons.Filled.PriorityHigh)
actual inline val PlatformIcons.Filled.Search: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Search)
actual inline val PlatformIcons.Filled.Settings: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Settings)
actual inline val PlatformIcons.Filled.Star: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Star)
actual inline val PlatformIcons.Filled.Sync: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Sync)
actual inline val PlatformIcons.Filled.TransferWithinAStation: Painter @Composable get() = rememberVectorPainter(Icons.Filled.TransferWithinAStation)
actual inline val PlatformIcons.Filled.Update: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Update)
actual inline val PlatformIcons.Filled.WrongLocation: Painter @Composable get() = rememberVectorPainter(Icons.Filled.WrongLocation)
actual inline val PlatformIcons.Filled.Palette: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Palette)
actual inline val PlatformIcons.Outlined.Add: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Add)
actual inline val PlatformIcons.Outlined.Bedtime: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Bedtime)
actual inline val PlatformIcons.Outlined.Bolt: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Bolt)
actual inline val PlatformIcons.Outlined.CalendarClock: Painter @Composable get() = painterResource(DrawableResource("baseline_calendar_clock_24.xml"))
actual inline val PlatformIcons.Outlined.Code: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Code)
actual inline val PlatformIcons.Outlined.Computer: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Computer)
actual inline val PlatformIcons.Outlined.CurrencyExchange: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.CurrencyExchange)
actual inline val PlatformIcons.Outlined.DepartureBoard: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DepartureBoard)
actual inline val PlatformIcons.Outlined.Delete: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Delete)
actual inline val PlatformIcons.Outlined.DeleteForever: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DeleteForever)
actual inline val PlatformIcons.Outlined.DirectionsBoat: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DirectionsBoat)
actual inline val PlatformIcons.Outlined.DirectionsBus: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DirectionsBus)
actual inline val PlatformIcons.Outlined.DoubleArrow: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DoubleArrow)
actual inline val PlatformIcons.Outlined.Download: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Download)
actual inline val PlatformIcons.Outlined.Edit: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Edit)
actual inline val PlatformIcons.Outlined.EmojiFlags: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.EmojiFlags)
actual inline val PlatformIcons.Outlined.Fingerprint: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Fingerprint)
actual inline val PlatformIcons.Outlined.FormatBold: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.FormatBold)
actual inline val PlatformIcons.Outlined.Fullscreen: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Fullscreen)
actual inline val PlatformIcons.Outlined.FullscreenExit: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.FullscreenExit)
actual inline val PlatformIcons.Outlined.History: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.History)
actual inline val PlatformIcons.Outlined.LightMode: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.LightMode)
actual inline val PlatformIcons.Outlined.LocationOff: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.LocationOff)
actual inline val PlatformIcons.Outlined.LocationOn: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.LocationOn)
actual inline val PlatformIcons.Outlined.Map: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Map)
actual inline val PlatformIcons.Outlined.MoreHoriz: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.MoreHoriz)
actual inline val PlatformIcons.Outlined.MoreVert: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.MoreVert)
actual inline val PlatformIcons.Outlined.MyLocation: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.MyLocation)
actual inline val PlatformIcons.Outlined.NearMe: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.NearMe)
actual inline val PlatformIcons.Outlined.NotificationImportant: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.NotificationImportant)
actual inline val PlatformIcons.Outlined.NotificationsActive: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.NotificationsActive)
actual inline val PlatformIcons.Outlined.NotificationsOff: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.NotificationsOff)
actual inline val PlatformIcons.Outlined.Paid: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Paid)
actual inline val PlatformIcons.Outlined.PinDrop: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.PinDrop)
actual inline val PlatformIcons.Outlined.Reorder: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Reorder)
actual inline val PlatformIcons.Outlined.Route: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Route)
actual inline val PlatformIcons.Outlined.Schedule: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Schedule)
actual inline val PlatformIcons.Outlined.Search: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Search)
actual inline val PlatformIcons.Outlined.Settings: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Settings)
actual inline val PlatformIcons.Outlined.Share: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Share)
actual inline val PlatformIcons.Outlined.Smartphone: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Smartphone)
actual inline val PlatformIcons.Outlined.Star: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Star)
actual inline val PlatformIcons.Outlined.StarOutline: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.StarOutline)
actual inline val PlatformIcons.Outlined.Start: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Start)
actual inline val PlatformIcons.Outlined.Streetview: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Streetview)
actual inline val PlatformIcons.Outlined.SwipeRightAlt: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.SwipeRightAlt)
actual inline val PlatformIcons.Outlined.SyncAlt: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.SyncAlt)
actual inline val PlatformIcons.Outlined.Tablet: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Tablet)
actual inline val PlatformIcons.Outlined.TextRotationNone: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.TextRotationNone)
actual inline val PlatformIcons.Outlined.Timer: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Timer)
actual inline val PlatformIcons.Outlined.Train: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Train)
actual inline val PlatformIcons.Outlined.TransferWithinAStation: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.TransferWithinAStation)
actual inline val PlatformIcons.Outlined.UTurnRight: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.UTurnRight)
actual inline val PlatformIcons.Outlined.Update: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Update)
actual inline val PlatformIcons.Outlined.Upload: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Upload)
actual inline val PlatformIcons.Outlined.Watch: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Watch)
actual inline val PlatformIcons.Outlined.LineEndCircle: Painter @Composable get() = painterResource(DrawableResource("baseline_line_end_circle_24.xml"))
actual inline val PlatformIcons.Outlined.MobileFriendly: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.MobileFriendly)
actual inline val PlatformIcons.Outlined.PhotoLibrary: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.PhotoLibrary)

actual val Painter.shouldBeTintedForIcons: Boolean get() = this is VectorPainter

@Composable
actual fun PlatformButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: ButtonColors,
    elevation: ButtonElevation?,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
actual fun PlatformFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: IconButtonColors,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
actual fun PlatformScaffold(
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    floatingActionButtonPosition: FabPosition,
    containerColor: Color?,
    contentColor: Color?,
    contentWindowInsets: WindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    val color = containerColor?: MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = color,
        contentColor = contentColor?: contentColorFor(color),
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}

@Composable
actual fun PlatformNavigationBar(
    modifier: Modifier,
    tonalElevation: Dp,
    windowInsets: WindowInsets,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = tonalElevation,
        windowInsets = windowInsets,
        content = content
    )
}

@Composable
actual fun RowScope.PlatformNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    colors: NavigationBarItemColors?,
    interactionSource: MutableInteractionSource
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = colors?: NavigationBarItemDefaults.colors(),
        interactionSource = interactionSource
    )
}

@Composable
actual fun PlatformText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    fontStyle: FontStyle?,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    letterSpacing: TextUnit,
    textDecoration: TextDecoration?,
    textAlign: TextAlign?,
    lineHeight: TextUnit,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    style: TextStyle
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
actual fun PlatformText(
    text: AnnotatedString,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    fontStyle: FontStyle?,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    letterSpacing: TextUnit,
    textDecoration: TextDecoration?,
    textAlign: TextAlign?,
    lineHeight: TextUnit,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    inlineContent: Map<String, InlineTextContent>,
    onTextLayout: (TextLayoutResult) -> Unit,
    style: TextStyle
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
actual fun PlatformIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color?
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint?: LocalContentColor.current
    )
}

@Composable
actual fun PlatformIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color?
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint?: LocalContentColor.current
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    iosDivider: (@Composable () -> Unit)?
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}

actual inline val platformComponentBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.background
actual inline val platformBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.background
actual inline val platformLocalContentColor: Color
    @Composable get() = LocalContentColor.current
actual inline val platformPrimaryContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
actual inline val platformSurfaceColor: Color
    @Composable get() = MaterialTheme.colorScheme.surface
actual inline val platformSurfaceContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer
actual inline val platformTopBarColor: Color
    @Composable get() = platformPrimaryContainerColor
actual inline val platformLocalTextStyle: TextStyle
    @Composable get() = LocalTextStyle.current
actual inline val platformExtraLargeShape: Shape
    @Composable get() = MaterialTheme.shapes.extraLarge
actual inline val platformLargeShape: Shape
    @Composable get() = MaterialTheme.shapes.large

@Composable
actual fun PlatformTabRow(
    selectedTabIndex: Int,
    modifier: Modifier,
    iosDivider: (@Composable () -> Unit)?,
    tabs: @Composable () -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        tabs = tabs
    )
}

@Composable
actual fun PlatformScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier,
    edgePadding: Dp,
    totalTabSize: Int,
    widestTabWidth: Dp,
    tabs: @Composable () -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        edgePadding = edgePadding,
        tabs = tabs
    )
}

@Composable
actual fun PlatformTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    text: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?
) {
    val styledContent: @Composable (() -> Unit)? = text?.let {
        @Composable {
            val style = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = text)
        }
    }
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource
    ) {
        TabBaselineLayout(
            text = styledContent,
            icon = icon
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: SheetState,
    sheetMaxWidth: Dp?,
    shape: Shape,
    tonalElevation: Dp,
    scrimColor: Color,
    desktopCloseColor: Color?,
    properties: ModalBottomSheetProperties,
    content: @Composable (ColumnScope.() -> Unit)
) {
    if (composePlatform.hasLargeScreen) {
        val margin = 70.dp
        val size = currentLocalWindowSize
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            sheetMaxWidth = sheetMaxWidth?: (size.width.equivalentDp - margin * 2).coerceAtLeast(BottomSheetDefaults.SheetMaxWidth),
            shape = shape,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = null,
            properties = properties,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = size.height.equivalentDp - margin),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                    PlatformButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.clearColors(),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest.invoke()
                            }
                        }
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(26.dp),
                            painter = PlatformIcons.Filled.Close,
                            tint = (desktopCloseColor?: MaterialTheme.colorScheme.onSurfaceVariant).copy(0.4F),
                            contentDescription = if (Shared.language == "en") "Close" else "關閉"
                        )
                    }
                }
            }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            sheetMaxWidth = sheetMaxWidth?: BottomSheetDefaults.SheetMaxWidth,
            shape = shape,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            properties = properties,
            content = content
        )
    }
}

@Composable
actual fun PlatformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
    interactionSource: MutableInteractionSource
) {
    Slider(
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        value = value,
        valueRange = valueRange
    )
}

@Composable
actual fun PlatformAlertDialog(
    iosSheetStyle: Boolean,
    iosCloseButton: Boolean,
    onDismissRequest: (DismissRequestType) -> Unit,
    confirmButton: @Composable () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest.invoke(DismissRequestType.CLICK_OUTSIDE) },
        confirmButton = {
            TextButton(
                modifier = Modifier.applyIf(confirmEnabled) { pointerHoverIcon(PointerIcon.Hand) },
                onClick = onConfirm,
                enabled = confirmEnabled
            ) {
                confirmButton.invoke()
            }
        },
        dismissButton = dismissButton?.let { {
            TextButton(
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                onClick = { onDismissRequest.invoke(DismissRequestType.BUTTON) }
            ) {
                it.invoke()
            }
        } },
        icon = icon,
        title = title,
        text = text
    )
}

actual fun Modifier.platformHorizontalDividerShadow(elevation: Dp): Modifier = this.shadow(elevation)
actual fun Modifier.platformVerticalDividerShadow(elevation: Dp): Modifier = this.shadow(elevation)

actual sealed interface PlatformDropdownMenuScope {
    data object Singleton: PlatformDropdownMenuScope
}

@Composable
actual fun PlatformDropdownMenu(
    modifier: Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable PlatformDropdownMenuScope.() -> Unit
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        content.invoke(PlatformDropdownMenuScope.Singleton)
    }
}

@Composable
actual fun PlatformDropdownMenuScope.PlatformDropdownMenuTitle(
    modifier: Modifier,
    title: @Composable (PaddingValues, TextUnit, Color) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        title.invoke(PaddingValues(horizontal = 10.dp), 17.sp, platformLocalContentColor.adjustAlpha(0.5F))
    }
}

@Composable
actual fun PlatformDropdownMenuScope.PlatformDropdownMenuDivider(
    modifier: Modifier
) {
    HorizontalDivider(modifier = modifier.padding(horizontal = 7.dp))
}

@Composable
actual fun PlatformDropdownMenuScope.PlatformDropdownMenuItem(
    text: @Composable (TextUnit) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    enabled: Boolean,
    colors: MenuItemColors?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource
) {
    DropdownMenuItem(
        text = { text.invoke(20.sp) },
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors?: MenuDefaults.itemColors(),
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}

@Composable
actual fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    thumbContent: @Composable (() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        interactionSource = interactionSource
    )
}

@Composable
actual fun PlatformFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    elevation: FloatingActionButtonElevation,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
actual fun PlatformExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    expanded: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    elevation: FloatingActionButtonElevation,
    interactionSource: MutableInteractionSource,
) {
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )
}

@Composable
actual fun PlatformFilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: IconToggleButtonColors,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit
) {
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
actual fun PlatformOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    placeholder: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    interactionSource: MutableInteractionSource
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource
    )
}

@Composable
actual fun PlatformLinearProgressIndicator(
    modifier: Modifier,
    color: Color?,
    trackColor: Color?,
    strokeCap: StrokeCap?
) {
    LinearProgressIndicator(
        modifier = modifier,
        color = color?: ProgressIndicatorDefaults.linearColor,
        trackColor = trackColor?: ProgressIndicatorDefaults.linearTrackColor,
        strokeCap = strokeCap?: ProgressIndicatorDefaults.LinearStrokeCap,
    )
}

@Composable
actual fun PlatformLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier,
    color: Color?,
    trackColor: Color?,
    strokeCap: StrokeCap?
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color?: ProgressIndicatorDefaults.linearColor,
        trackColor = trackColor?: ProgressIndicatorDefaults.linearTrackColor,
        strokeCap = strokeCap?: ProgressIndicatorDefaults.LinearStrokeCap,
    )
}

@Composable
actual fun PlatformCircularProgressIndicator(
    modifier: Modifier,
    color: Color?,
    strokeWidth: Dp?,
    trackColor: Color?,
    strokeCap: StrokeCap?
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color?: ProgressIndicatorDefaults.circularColor,
        strokeWidth = strokeWidth?: ProgressIndicatorDefaults.CircularStrokeWidth,
        trackColor = trackColor?: ProgressIndicatorDefaults.circularIndeterminateTrackColor,
        strokeCap = strokeCap?: ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
    )
}

actual val platformShowDownloadAppBottomSheet: (() -> Unit)? = {
    CoroutineScope(Dispatchers.Main).launch {
        setDownloadAppSheetVisible(isAppleDevice(), true, Shared.theme == Theme.DARK, isWasmSupported())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TooltipScope.PlatformPlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    shape: Shape?,
    contentColor: Color?,
    containerColor: Color?,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    PlainTooltip(
        modifier = modifier,
        caretSize = caretSize,
        shape = shape?: TooltipDefaults.plainTooltipContainerShape,
        contentColor = contentColor?: TooltipDefaults.plainTooltipContentColor,
        containerColor = containerColor?: TooltipDefaults.plainTooltipContainerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content
    )
}

@Composable
actual fun PlatformCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    colors: CheckboxColors?,
    interactionSource: MutableInteractionSource?
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors?: CheckboxDefaults.colors(),
        interactionSource = null
    )
}

@Composable
actual fun PlatformRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    colors: RadioButtonColors?,
    interactionSource: MutableInteractionSource?
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors?: RadioButtonDefaults.colors(),
        interactionSource = interactionSource
    )
}