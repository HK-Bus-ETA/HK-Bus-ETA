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

package com.loohp.hkbuseta.appcontext

import com.loohp.hkbuseta.common.utils.Immutable


enum class PlatformType {
    PHONE, TABLET, COMPUTER
}

@Immutable
sealed class ComposePlatform(
    val displayName: String,
    val type: PlatformType,
    val hasMouse: Boolean,
    val hasKeyboard: Boolean,
    val hasLocation: Boolean,
    val hasBackgroundLocation: Boolean,
    val mayHaveWatch: Boolean,
    val supportPip: Boolean,
    val applePlatform: Boolean,
    val appleEnvironment: Boolean,
    val isMobileAppRunningOnDesktop: Boolean,
    val hasLargeScreen: Boolean,
    val browserEnvironment: Boolean
) {
    @Immutable
    class AndroidPlatform(tablet: Boolean): ComposePlatform(
        displayName = "Android${if (tablet) " (Tablet)" else ""}",
        type = if (tablet) PlatformType.TABLET else PlatformType.PHONE,
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = true,
        supportPip = true,
        applePlatform = false,
        appleEnvironment = false,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = tablet,
        browserEnvironment = false
    )
    @Immutable
    class IOSPlatform(ipad: Boolean): ComposePlatform(
        displayName = "iOS${if (ipad) " (iPad)" else ""}",
        type = if (ipad) PlatformType.TABLET else PlatformType.PHONE,
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = !ipad,
        supportPip = false,
        applePlatform = true,
        appleEnvironment = true,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = ipad,
        browserEnvironment = false
    )
    @Immutable
    data object MacAppleSiliconPlatform: ComposePlatform(
        displayName = "MacOS (Apple Silicon)",
        type = PlatformType.COMPUTER,
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = true,
        appleEnvironment = true,
        isMobileAppRunningOnDesktop = true,
        hasLargeScreen = true,
        browserEnvironment = false
    )
    @Immutable
    class DesktopPlatform(isApple: Boolean): ComposePlatform(
        displayName = "Desktop${if (isApple) " (MacOS)" else ""}",
        type = PlatformType.COMPUTER,
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = false,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = true,
        browserEnvironment = false
    )
    @Immutable
    class WebDesktopPlatform(isApple: Boolean): ComposePlatform(
        displayName = "Web Desktop${if (isApple) " (MacOS)" else ""}",
        type = PlatformType.COMPUTER,
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = true,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = true,
        hasLargeScreen = true,
        browserEnvironment = true
    )
    @Immutable
    class WebMobilePlatform(isApple: Boolean): ComposePlatform(
        displayName = "Web Mobile${if (isApple) " (iOS)" else ""}",
        type = PlatformType.PHONE,
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = false,
        browserEnvironment = true
    )
}

expect val composePlatform: ComposePlatform