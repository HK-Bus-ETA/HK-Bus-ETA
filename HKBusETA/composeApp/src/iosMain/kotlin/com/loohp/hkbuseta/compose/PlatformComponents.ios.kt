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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.NoTransfer
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DepartureBoard
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CaretProperties
import androidx.compose.material3.CaretScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.EqualityDelegate
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageScope
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.theme.AppTheme
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.UIImagePainter
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.asPainter
import com.loohp.hkbuseta.utils.equivalentDp
import io.github.alexzhirkevich.LocalContentColor
import io.github.alexzhirkevich.LocalTextStyle
import io.github.alexzhirkevich.cupertino.CupertinoActionSheet
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoBorderedTextField
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoDividerDefaults
import io.github.alexzhirkevich.cupertino.CupertinoDropdownMenuDefaults
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBar
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBarDefaults
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBarItem
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoScaffoldDefaults
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControl
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControlTab
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextFieldDefaults
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBarDefaults
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.ProvideTextStyle
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemGray5
import org.jetbrains.compose.resources.painterResource
import platform.UIKit.UIImage
import platform.UIKit.UIImageConfiguration
import kotlin.math.max
import androidx.compose.material3.LocalContentColor as LocalMaterialContentColor
import androidx.compose.material3.LocalTextStyle as LocalMaterialTextStyle


inline val unknownImagePlaceholderPainter: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.BrokenImage)

@Composable
inline fun rememberImagePainter(name: String, scaleX: Float = 1F, scaleY: Float = scaleX): Painter {
    return UIImage.imageNamed(name)
        ?.asPainter(isIcon = true, scaleX = scaleX, scaleY = scaleY)
        ?: unknownImagePlaceholderPainter
}
@Composable
inline fun rememberImagePainter(name: String, vararg fallbacks: String, scaleX: Float = 1F, scaleY: Float = scaleX): Painter {
    return (sequenceOf(name) + fallbacks.asSequence())
        .firstNotNullOfOrNull { UIImage.imageNamed(it) }
        ?.asPainter(isIcon = true, scaleX = scaleX, scaleY = scaleY)
        ?: unknownImagePlaceholderPainter
}
@Composable
inline fun rememberSystemImagePainter(name: String, scaleX: Float = 1F, scaleY: Float = scaleX): Painter {
    return UIImage.systemImageNamed(name)
        ?.asPainter(isIcon = true, scaleX = scaleX, scaleY = scaleY)
        ?: unknownImagePlaceholderPainter
}
@Composable
inline fun rememberSystemImagePainter(name: String, vararg fallbacks: String, scaleX: Float = 1F, scaleY: Float = scaleX): Painter {
    return (sequenceOf(name) + fallbacks.asSequence())
        .firstNotNullOfOrNull { UIImage.systemImageNamed(it) }
        ?.asPainter(isIcon = true, scaleX = scaleX, scaleY = scaleY)
        ?: unknownImagePlaceholderPainter
}
@Composable
inline fun rememberSystemImagePainter(name: String, configuration: UIImageConfiguration?): Painter {
    return UIImage.systemImageNamed(name, configuration)?.asPainter(isIcon = true)?: unknownImagePlaceholderPainter
}

actual inline val PlatformIcons.AutoMirrored.Filled.ArrowBack: Painter @Composable get() = rememberSystemImagePainter("arrow.backward")
actual inline val PlatformIcons.AutoMirrored.Filled.OpenInNew: Painter @Composable get() = rememberSystemImagePainter("square.on.square")
actual inline val PlatformIcons.AutoMirrored.Filled.Sort: Painter @Composable get() = rememberSystemImagePainter("slider.horizontal.3")
actual inline val PlatformIcons.AutoMirrored.Outlined.Accessible: Painter @Composable get() = rememberSystemImagePainter("accessibility")
actual inline val PlatformIcons.AutoMirrored.Outlined.Backspace: Painter @Composable get() = rememberSystemImagePainter("delete.backward")
actual inline val PlatformIcons.AutoMirrored.Outlined.AltRoute: Painter @Composable get() = rememberSystemImagePainter("arrow.triangle.branch")
actual inline val PlatformIcons.Filled.ArrowDownward: Painter @Composable get() = rememberSystemImagePainter("arrow.down")
actual inline val PlatformIcons.Filled.ArrowUpward: Painter @Composable get() = rememberSystemImagePainter("arrow.up")
actual inline val PlatformIcons.Filled.Bolt: Painter @Composable get() = rememberSystemImagePainter("bolt.fill")
actual inline val PlatformIcons.Filled.CheckCircle: Painter @Composable get() = rememberSystemImagePainter("checkmark.circle.fill")
actual inline val PlatformIcons.Filled.ChevronLeft: Painter @Composable get() = rememberSystemImagePainter("chevron.left")
actual inline val PlatformIcons.Filled.ChevronRight: Painter @Composable get() = rememberSystemImagePainter("chevron.right")
actual inline val PlatformIcons.Filled.FirstPage: Painter @Composable get() = rememberSystemImagePainter("arrow.left.to.line")
actual inline val PlatformIcons.Filled.LastPage: Painter @Composable get() = rememberSystemImagePainter("arrow.right.to.line")
actual inline val PlatformIcons.Filled.ArrowDropDown: Painter @Composable get() = rememberSystemImagePainter("arrowtriangle.down.fill", 0.5F)
actual inline val PlatformIcons.Filled.Close: Painter @Composable get() = rememberSystemImagePainter("xmark")
actual inline val PlatformIcons.Filled.Dangerous: Painter @Composable get() = rememberSystemImagePainter("xmark.octagon")
actual inline val PlatformIcons.Filled.DarkMode: Painter @Composable get() = rememberSystemImagePainter("moon.fill")
actual inline val PlatformIcons.Filled.DeleteForever: Painter @Composable get() = rememberSystemImagePainter("trash.fill")
actual inline val PlatformIcons.Filled.EditNote: Painter @Composable get() = rememberSystemImagePainter("square.and.pencil")
actual inline val PlatformIcons.Filled.Error: Painter @Composable get() = rememberSystemImagePainter("exclamationmark.circle.fill")
actual inline val PlatformIcons.Filled.Forest: Painter @Composable get() = rememberVectorPainter(Icons.Filled.Forest)
actual inline val PlatformIcons.Filled.History: Painter @Composable get() = rememberSystemImagePainter("clock.arrow.circlepath")
actual inline val PlatformIcons.Filled.Info: Painter @Composable get() = rememberSystemImagePainter("info.circle.fill")
actual inline val PlatformIcons.Filled.Translate: Painter @Composable get() = if (Shared.language == "en") rememberImagePainter("character.textbox.en") else rememberSystemImagePainter("character.textbox.zh")
actual inline val PlatformIcons.Filled.LocationDisabled: Painter @Composable get() = rememberVectorPainter(Icons.Filled.LocationDisabled)
actual inline val PlatformIcons.Filled.Map: Painter @Composable get() = rememberSystemImagePainter("map.fill")
actual inline val PlatformIcons.Filled.NearMe: Painter @Composable get() = rememberSystemImagePainter("location.fill")
actual inline val PlatformIcons.Filled.NearMeDisabled: Painter @Composable get() = rememberSystemImagePainter("location.slash.fill")
actual inline val PlatformIcons.Filled.NoTransfer: Painter @Composable get() = rememberVectorPainter(Icons.Filled.NoTransfer)
actual inline val PlatformIcons.Filled.PinDrop: Painter @Composable get() = rememberSystemImagePainter("mappin.circle.fill")
actual inline val PlatformIcons.Filled.PhotoCamera: Painter @Composable get() = rememberSystemImagePainter("camera.fill")
actual inline val PlatformIcons.Filled.PriorityHigh: Painter @Composable get() = rememberSystemImagePainter("exclamationmark")
actual inline val PlatformIcons.Filled.Search: Painter @Composable get() = rememberSystemImagePainter("magnifyingglass")
actual inline val PlatformIcons.Filled.Settings: Painter @Composable get() = rememberSystemImagePainter("gearshape.fill")
actual inline val PlatformIcons.Filled.Star: Painter @Composable get() = rememberSystemImagePainter("star.fill")
actual inline val PlatformIcons.Filled.Sync: Painter @Composable get() = rememberSystemImagePainter("arrow.triangle.2.circlepath")
actual inline val PlatformIcons.Filled.TransferWithinAStation: Painter @Composable get() = rememberSystemImagePainter("figure.walk")
actual inline val PlatformIcons.Filled.Update: Painter @Composable get() = rememberSystemImagePainter("arrow.triangle.2.circlepath")
actual inline val PlatformIcons.Filled.WrongLocation: Painter @Composable get() = rememberSystemImagePainter("mappin.slash")
actual inline val PlatformIcons.Filled.Palette: Painter @Composable get() = rememberSystemImagePainter("paintpalette.fill")
actual inline val PlatformIcons.Outlined.Add: Painter @Composable get() = rememberSystemImagePainter("plus")
actual inline val PlatformIcons.Outlined.Bedtime: Painter @Composable get() = rememberSystemImagePainter("moon")
actual inline val PlatformIcons.Outlined.Bolt: Painter @Composable get() = rememberSystemImagePainter("bolt")
actual inline val PlatformIcons.Outlined.CalendarClock: Painter @Composable get() = painterResource(DrawableResource("baseline_calendar_clock_24.xml"))
actual inline val PlatformIcons.Outlined.Code: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.Code)
actual inline val PlatformIcons.Outlined.CurrencyExchange: Painter @Composable get() = rememberSystemImagePainter("dollarsign.arrow.circlepath")
actual inline val PlatformIcons.Outlined.DepartureBoard: Painter @Composable get() = rememberVectorPainter(Icons.Outlined.DepartureBoard)
actual inline val PlatformIcons.Outlined.Delete: Painter @Composable get() = rememberSystemImagePainter("trash")
actual inline val PlatformIcons.Outlined.DeleteForever: Painter @Composable get() = rememberSystemImagePainter("trash")
actual inline val PlatformIcons.Outlined.DirectionsBoat: Painter @Composable get() = rememberSystemImagePainter("ferry")
actual inline val PlatformIcons.Outlined.DoubleArrow: Painter @Composable get() = rememberSystemImagePainter("chevron.right.2")
actual inline val PlatformIcons.Outlined.Download: Painter @Composable get() = rememberSystemImagePainter("arrow.down.to.line")
actual inline val PlatformIcons.Outlined.Edit: Painter @Composable get() = rememberSystemImagePainter("pencil")
actual inline val PlatformIcons.Outlined.EmojiFlags: Painter @Composable get() = rememberSystemImagePainter("flag")
actual inline val PlatformIcons.Outlined.Fingerprint: Painter @Composable get() = rememberSystemImagePainter("person.fill")
actual inline val PlatformIcons.Outlined.FormatBold: Painter @Composable get() = rememberSystemImagePainter("bold")
actual inline val PlatformIcons.Outlined.Fullscreen: Painter @Composable get() = rememberSystemImagePainter("arrow.up.left.and.arrow.down.right")
actual inline val PlatformIcons.Outlined.FullscreenExit: Painter @Composable get() = rememberSystemImagePainter("arrow.down.right.and.arrow.up.left")
actual inline val PlatformIcons.Outlined.History: Painter @Composable get() = rememberSystemImagePainter("clock.arrow.circlepath")
actual inline val PlatformIcons.Outlined.LightMode: Painter @Composable get() = rememberSystemImagePainter("sun.max")
actual inline val PlatformIcons.Outlined.LocationOff: Painter @Composable get() = rememberSystemImagePainter("location.slash")
actual inline val PlatformIcons.Outlined.LocationOn: Painter @Composable get() = rememberSystemImagePainter("location")
actual inline val PlatformIcons.Outlined.Map: Painter @Composable get() = rememberSystemImagePainter("map")
actual inline val PlatformIcons.Outlined.MoreHoriz: Painter @Composable get() = rememberSystemImagePainter("ellipsis")
actual inline val PlatformIcons.Outlined.MoreVert: Painter @Composable get() = rememberSystemImagePainter("arrow.down")
actual inline val PlatformIcons.Outlined.MyLocation: Painter @Composable get() = rememberSystemImagePainter("dot.scope", "scope")
actual inline val PlatformIcons.Outlined.NearMe: Painter @Composable get() = rememberSystemImagePainter("location")
actual inline val PlatformIcons.Outlined.NotificationImportant: Painter @Composable get() = rememberSystemImagePainter("bell.badge")
actual inline val PlatformIcons.Outlined.NotificationsActive: Painter @Composable get() = rememberSystemImagePainter("bell")
actual inline val PlatformIcons.Outlined.NotificationsOff: Painter @Composable get() = rememberSystemImagePainter("bell.slash")
actual inline val PlatformIcons.Outlined.Paid: Painter @Composable get() = rememberSystemImagePainter("dollarsign.circle")
actual inline val PlatformIcons.Outlined.PinDrop: Painter @Composable get() = rememberSystemImagePainter("mappin.circle")
actual inline val PlatformIcons.Outlined.Reorder: Painter @Composable get() = rememberSystemImagePainter("line.3.horizontal")
actual inline val PlatformIcons.Outlined.Schedule: Painter @Composable get() = rememberSystemImagePainter("clock")
actual inline val PlatformIcons.Outlined.Search: Painter @Composable get() = rememberSystemImagePainter("magnifyingglass")
actual inline val PlatformIcons.Outlined.Settings: Painter @Composable get() = rememberSystemImagePainter("gearshape")
actual inline val PlatformIcons.Outlined.Share: Painter @Composable get() = rememberSystemImagePainter("square.and.arrow.up")
actual inline val PlatformIcons.Outlined.Smartphone: Painter @Composable get() = rememberSystemImagePainter("smartphone")
actual inline val PlatformIcons.Outlined.Star: Painter @Composable get() = rememberSystemImagePainter("star.fill")
actual inline val PlatformIcons.Outlined.StarOutline: Painter @Composable get() = rememberSystemImagePainter("star")
actual inline val PlatformIcons.Outlined.Start: Painter @Composable get() = rememberSystemImagePainter("play")
actual inline val PlatformIcons.Outlined.Streetview: Painter @Composable get() = rememberSystemImagePainter("road.lanes")
actual inline val PlatformIcons.Outlined.SwipeRightAlt: Painter @Composable get() = rememberSystemImagePainter("arrow.right.circle")
actual inline val PlatformIcons.Outlined.SyncAlt: Painter @Composable get() = rememberSystemImagePainter("arrow.left.arrow.right")
actual inline val PlatformIcons.Outlined.TextRotationNone: Painter @Composable get() = rememberSystemImagePainter("textformat.abc.dottedunderline")
actual inline val PlatformIcons.Outlined.Timer: Painter @Composable get() = rememberSystemImagePainter("timer")
actual inline val PlatformIcons.Outlined.Train: Painter @Composable get() = rememberSystemImagePainter("train.side.front.car")
actual inline val PlatformIcons.Outlined.TransferWithinAStation: Painter @Composable get() = rememberSystemImagePainter("figure.walk")
actual inline val PlatformIcons.Outlined.UTurnRight: Painter @Composable get() = rememberSystemImagePainter("arrow.uturn.down")
actual inline val PlatformIcons.Outlined.Update: Painter @Composable get() = rememberSystemImagePainter("arrow.triangle.2.circlepath")
actual inline val PlatformIcons.Outlined.Upload: Painter @Composable get() = rememberSystemImagePainter("arrow.up.to.line")
actual inline val PlatformIcons.Outlined.Watch: Painter @Composable get() = rememberSystemImagePainter("applewatch")
actual inline val PlatformIcons.Outlined.LineEndCircle: Painter @Composable get() = rememberSystemImagePainter("arrow.forward.to.line.circle")
actual inline val PlatformIcons.Outlined.MobileFriendly: Painter @Composable get() = rememberSystemImagePainter("smartphone")
actual inline val PlatformIcons.Outlined.PhotoLibrary: Painter @Composable get() = rememberSystemImagePainter("photo.stack")

actual val Painter.shouldBeTintedForIcons: Boolean get() = this is VectorPainter || (this is UIImagePainter && isIcon)

@OptIn(ExperimentalCupertinoApi::class)
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
    CupertinoButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = shape,
        colors = CupertinoButtonDefaults.plainButtonColors(
            containerColor = colors.containerColor,
            contentColor = colors.contentColor,
            disabledContainerColor = colors.disabledContainerColor,
            disabledContentColor = colors.disabledContentColor
        ),
        enabled = enabled,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content.invoke(this)
            }
        }
    )
}

@OptIn(ExperimentalCupertinoApi::class)
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
    CupertinoButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = shape,
        colors = CupertinoButtonDefaults.tintedButtonColors(
            containerColor = colors.containerColor,
            contentColor = colors.contentColor,
            disabledContainerColor = colors.disabledContainerColor,
            disabledContentColor = colors.disabledContentColor
        ),
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        content = {
            content.invoke()
        }
    )
}

@OptIn(ExperimentalCupertinoApi::class)
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
    CupertinoScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = when (floatingActionButtonPosition) {
            FabPosition.Center -> io.github.alexzhirkevich.cupertino.FabPosition.Center
            else -> io.github.alexzhirkevich.cupertino.FabPosition.End
        },
        containerColor = containerColor?: CupertinoScaffoldDefaults.containerColor,
        contentColor = contentColor?: CupertinoScaffoldDefaults.contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformNavigationBar(
    modifier: Modifier,
    tonalElevation: Dp,
    windowInsets: WindowInsets,
    content: @Composable RowScope.() -> Unit
) {
    CupertinoNavigationBar(
        modifier = modifier,
        windowInsets = windowInsets,
        content = content
    )
}

@OptIn(ExperimentalCupertinoApi::class)
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
    CupertinoNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = colors?.run {
            CupertinoNavigationBarDefaults.itemColors(
                selectedIconColor = selectedIconColor.takeOrElse { CupertinoTheme.colorScheme.accent },
                selectedTextColor = selectedTextColor.takeOrElse { CupertinoTheme.colorScheme.accent },
                unselectedIconColor = unselectedIconColor.takeOrElse { CupertinoTheme.colorScheme.secondaryLabel },
                unselectedTextColor = unselectedTextColor.takeOrElse { CupertinoTheme.colorScheme.secondaryLabel },
                disabledIconColor = disabledIconColor.takeOrElse { CupertinoTheme.colorScheme.tertiaryLabel },
                disabledTextColor = disabledTextColor.takeOrElse { CupertinoTheme.colorScheme.tertiaryLabel },
            )
        }?: CupertinoNavigationBarDefaults.itemColors(),
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
    CupertinoText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign?: TextAlign.Unspecified,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout?: {},
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
    CupertinoText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign?: TextAlign.Unspecified,
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
    CupertinoIcon(
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
    CupertinoIcon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint?: LocalContentColor.current
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    iosDivider: (@Composable () -> Unit)?
) {
    var navigationIconWidth by remember(navigationIcon) { mutableIntStateOf(-1) }
    var actionsWidth by remember(actions) { mutableIntStateOf(-1) }
    CupertinoTopAppBar(
        title = {
            val padding = max(navigationIconWidth, actionsWidth)
            if (padding >= 0) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp, horizontal = padding.equivalentDp)
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    title.invoke()
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .onSizeChanged { navigationIconWidth = it.width }
            ) {
                navigationIcon.invoke()
            }
        },
        actions = {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .onSizeChanged { actionsWidth = it.width }
            ) {
                actions.invoke(this)
            }
        },
        windowInsets = windowInsets,
        divider = iosDivider?: { CupertinoTopAppBarDefaults.divider() }
    )
}

actual inline val platformComponentBackgroundColor: Color
    @Composable get() = CupertinoTheme.colorScheme.run {
        if (Shared.theme.isDarkMode) secondarySystemBackground else tertiarySystemBackground
    }
actual inline val platformBackgroundColor: Color
    @Composable get() = CupertinoTheme.colorScheme.systemBackground
actual inline val platformLocalContentColor: Color
    @Composable get() = LocalContentColor.current
actual inline val platformPrimaryContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
actual inline val platformSurfaceColor: Color
    @Composable get() = CupertinoTheme.colorScheme.systemBackground
actual inline val platformSurfaceContainerColor: Color
    @Composable get() = CupertinoTheme.colorScheme.systemBackground
actual inline val platformTopBarColor: Color
    @Composable get() = CupertinoTheme.colorScheme.systemBackground
actual inline val platformLocalTextStyle: TextStyle
    @Composable get() = LocalTextStyle.current
actual inline val platformExtraLargeShape: Shape
    @Composable get() = CupertinoTheme.shapes.extraLarge
actual inline val platformLargeShape: Shape
    @Composable get() = CupertinoTheme.shapes.large

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformTabRow(
    selectedTabIndex: Int,
    modifier: Modifier,
    iosDivider: (@Composable () -> Unit)?,
    tabs: @Composable () -> Unit
) {
    Column {
        CupertinoSegmentedControl(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier.background(platformBackgroundColor),
            tabs = tabs
        )
        iosDivider?.invoke()
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier,
    edgePadding: Dp,
    totalTabSize: Int,
    widestTabWidth: Dp,
    tabs: @Composable () -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scroll)
    ) {
        CupertinoSegmentedControl(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier
                .padding(horizontal = edgePadding)
                .width((widestTabWidth + 50.dp) * totalTabSize),
            tabs = tabs
        )
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    text: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?
) {
    CupertinoSegmentedControlTab(
        isSelected = selected,
        onClick = onClick,
        modifier = modifier
            .applyIfNotNull(icon) { padding(bottom = 3.dp, top = 5.dp) }
            .pointerHoverIcon(PointerIcon.Hand),
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                icon?.invoke()
                text?.invoke()
            }
        },
        interactionSource = interactionSource
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: SheetState,
    sheetMaxWidth: Dp?,
    shape: Shape,
    tonalElevation: Dp,
    scrimColor: Color,
    windowInsets: WindowInsets,
    desktopCloseColor: Color?,
    properties: ModalBottomSheetProperties,
    content: @Composable ColumnScope.() -> Unit,
) {
    LaunchedEffect (Unit) {
        sheetState.show()
    }
    CupertinoActionSheet(
        visible = sheetState.isVisible,
        onDismissRequest = { /* do nothing */ },
        content = {
            Box(
                modifier = Modifier.heightIn(max = (currentLocalWindowSize.height / 5 * 4).equivalentDp)
            ) {
                Column(
                    modifier = modifier
                ) {
                    content.invoke(this)
                }
            }
        },
        buttons = {
            action(
                onClick = onDismissRequest,
                title = {
                    PlatformText(text = if (Shared.language == "en") "Close" else "關閉")
                }
            )
        }
    )
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
    CupertinoSlider(
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

@OptIn(ExperimentalCupertinoApi::class, ExperimentalComposeUiApi::class)
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
    if (iosSheetStyle) {
        CupertinoActionSheet(
            visible = true,
            onDismissRequest = { /* do nothing */ },
            title = {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 21F.sp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        icon?.invoke()
                        title?.invoke()
                    }
                }
            },
            content = {
                val window = LocalWindowInfo.current.containerSize
                Box(
                    modifier = Modifier
                        .padding(vertical = 15.dp)
                        .heightIn(max = (window.height / 3 * 2).equivalentDp)
                ) {
                    text?.invoke()
                }
            },
            buttons = {
                if (iosCloseButton) {
                    action(
                        onClick = { onDismissRequest.invoke(DismissRequestType.CLICK_OUTSIDE) },
                        title = {
                            PlatformText(text = if (Shared.language == "en") "Close" else "關閉")
                        }
                    )
                }
                if (dismissButton != null) {
                    action(
                        onClick = { onDismissRequest.invoke(DismissRequestType.BUTTON) },
                        title = dismissButton
                    )
                }
                action(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    title = if (confirmEnabled) confirmButton else { {
                        ProvideTextStyle(
                            value = TextStyle(color = ButtonDefaults.buttonColors().disabledContentColor)
                        ) {
                            confirmButton.invoke()
                        }
                    } }
                )
            }
        )
    } else {
        CupertinoAlertDialog(
            onDismissRequest = { /* do nothing */ },
            title = {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 21F.sp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        icon?.invoke()
                        title?.invoke()
                    }
                }
            },
            message = {
                Box(
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    text?.invoke()
                }
            },
            buttons = {
                if (iosCloseButton) {
                    action(
                        onClick = { onDismissRequest.invoke(DismissRequestType.CLICK_OUTSIDE) },
                        title = {
                            PlatformText(text = if (Shared.language == "en") "Close" else "關閉")
                        }
                    )
                }
                if (dismissButton != null) {
                    action(
                        onClick = { onDismissRequest.invoke(DismissRequestType.BUTTON) },
                        title = dismissButton
                    )
                }
                action(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    title = if (confirmEnabled) confirmButton else { {
                        ProvideTextStyle(
                            value = TextStyle(color = ButtonDefaults.buttonColors().disabledContentColor)
                        ) {
                            confirmButton.invoke()
                        }
                    } }
                )
            }
        )
    }
}

actual fun Modifier.platformHorizontalDividerShadow(elevation: Dp): Modifier = composed {
    val color = CupertinoDividerDefaults.color
    val targetThickness = (1f / LocalDensity.current.density).dp
    drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = Offset(0F, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = targetThickness.toPx()
        )
    }
}
actual fun Modifier.platformVerticalDividerShadow(elevation: Dp): Modifier = composed {
    val color = CupertinoDividerDefaults.color
    val targetThickness = (1f / LocalDensity.current.density).dp
    drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = Offset(size.width, 0F),
            end = Offset(size.width, size.height),
            strokeWidth = targetThickness.toPx()
        )
    }
}

@Composable
actual fun PlatformDropdownMenu(
    modifier: Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        modifier = modifier.background(CupertinoDropdownMenuDefaults.ContainerColor),
        expanded = expanded,
        onDismissRequest = if (composePlatform.isMobileAppRunningOnDesktop) ({ /* do nothing */ }) else onDismissRequest,
        content = {
            AppTheme(
                useDarkTheme = Shared.theme.isDarkMode,
                customColor = Shared.color?.let { Color(it) }
            ) {
                content.invoke(this)
            }
        }
    )
}

@Composable
actual fun PlatformDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    enabled: Boolean,
    colors: MenuItemColors,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource
) {
    val ripple = LocalRippleTheme.current
    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleTheme
    ) {
        val pressed by interactionSource.collectIsPressedAsState()
        val animatedAlpha by animateFloatAsState(
            targetValue = if (pressed) 0.33F else 1f
        )
        DropdownMenuItem(
            text = {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (pressed) 0.33F else animatedAlpha
                    }
                ) {
                    CompositionLocalProvider(LocalRippleTheme provides ripple) {
                        text.invoke()
                    }
                }
            },
            onClick = onClick,
            modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = colors,
            contentPadding = contentPadding,
            interactionSource = interactionSource
        )
    }
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
    CupertinoSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        interactionSource = interactionSource
    )
}

private object NoRippleTheme: RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
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
    val ripple = LocalRippleTheme.current
    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleTheme
    ) {
        val pressed by interactionSource.collectIsPressedAsState()
        val animatedAlpha by animateFloatAsState(
            targetValue = if (pressed) 0.33F else 1f
        )
        val borderColor = CupertinoDividerDefaults.color
        Surface(
            onClick = onClick,
            modifier = modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .semantics { role = Role.Button },
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor),
            interactionSource = interactionSource,
            content = {
                Box(
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = 56.dp,
                            minHeight = 56.dp,
                        )
                        .graphicsLayer {
                            alpha = if (pressed) 0.33F else animatedAlpha
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(
                        LocalRippleTheme provides ripple,
                        androidx.compose.material3.LocalContentColor provides contentColor
                    ) {
                        content.invoke()
                    }
                }
            }
        )
    }
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
    val ripple = LocalRippleTheme.current
    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleTheme
    ) {
        val pressed by interactionSource.collectIsPressedAsState()
        val animatedAlpha by animateFloatAsState(
            targetValue = if (pressed) 0.33F else 1f
        )
        ExtendedFloatingActionButton(
            text = {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (pressed) 0.33F else animatedAlpha
                    }
                ) {
                    CompositionLocalProvider(LocalRippleTheme provides ripple) {
                        text.invoke()
                    }
                }
            },
            icon = {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (pressed) 0.33F else animatedAlpha
                    }
                ) {
                    CompositionLocalProvider(LocalRippleTheme provides ripple) {
                        icon.invoke()
                    }
                }
            },
            onClick = onClick,
            modifier = modifier
                .border(
                    border = BorderStroke(1.dp, CupertinoDividerDefaults.color),
                    shape = shape
                )
                .pointerHoverIcon(PointerIcon.Hand),
            expanded = expanded,
            shape = shape,
            containerColor = containerColor,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            interactionSource = interactionSource
        )
    }
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
    val ripple = LocalRippleTheme.current
    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleTheme
    ) {
        val pressed by interactionSource.collectIsPressedAsState()
        val animatedAlpha by animateFloatAsState(
            targetValue = if (pressed) 0.33F else 1f
        )
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
            enabled = enabled,
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
            content = {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (pressed) 0.33F else animatedAlpha
                    }
                ) {
                    CompositionLocalProvider(LocalRippleTheme provides ripple) {
                        content.invoke()
                    }
                }
            }
        )
    }
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
    CupertinoBorderedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder?.let { {
            Box(
                modifier = Modifier.defaultMinSize(
                    minWidth = CupertinoTextFieldDefaults.MinWidth,
                    minHeight = CupertinoTextFieldDefaults.MinHeight
                ),
                contentAlignment = Alignment.CenterStart
            ) {
                it.invoke()
            }
        } },
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
    val indicatorColor = color?: LocalContentColor.current
    LinearProgressIndicator(
        modifier = modifier.height(15.dp),
        color = indicatorColor,
        trackColor = trackColor?: indicatorColor.adjustBrightness(0.4F),
        strokeCap = strokeCap?: StrokeCap.Round,
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
    val indicatorColor = color?: LocalContentColor.current
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier.height(15.dp),
        color = indicatorColor,
        trackColor = indicatorColor.adjustBrightness(0.4F),
        strokeCap = strokeCap?: StrokeCap.Round,
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
    val indicatorColor = color?: LocalContentColor.current
    CircularProgressIndicator(
        modifier = modifier,
        color = indicatorColor,
        strokeWidth = strokeWidth?: ProgressIndicatorDefaults.CircularStrokeWidth,
        trackColor = indicatorColor.adjustBrightness(0.4F),
        strokeCap = strokeCap?: StrokeCap.Round,
    )
}

actual val platformShowDownloadAppBottomSheet: (() -> Unit)? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CaretScope.PlatformPlainTooltip(
    modifier: Modifier,
    caretProperties: CaretProperties?,
    shape: Shape?,
    contentColor: Color?,
    containerColor: Color?,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    PlainTooltip(
        modifier = modifier,
        caretProperties = caretProperties,
        shape = shape?: TooltipDefaults.plainTooltipContainerShape,
        contentColor = contentColor?: CupertinoTheme.colorScheme.label,
        containerColor = containerColor?: CupertinoColors.systemGray5,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides LocalMaterialContentColor.current,
                LocalTextStyle provides LocalTextStyle.current.copy(fontWeight = LocalMaterialTextStyle.current.fontWeight)
            ) {
                content.invoke()
            }
        }
    )
}