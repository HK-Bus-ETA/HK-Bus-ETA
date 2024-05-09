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


@Immutable
sealed class ComposePlatform(
    val hasMouse: Boolean,
    val hasKeyboard: Boolean,
    val hasLocation: Boolean,
    val hasBackgroundLocation: Boolean,
    val mayHaveWatch: Boolean,
    val supportPip: Boolean,
    val applePlatform: Boolean,
    val appleEnvironment: Boolean,
    val isMobileAppRunningOnDesktop: Boolean,
    val hasLargeScreen: Boolean
) {
    @Immutable
    data object AndroidPlatform: ComposePlatform(
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = true,
        supportPip = true,
        applePlatform = false,
        appleEnvironment = false,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = false
    )
    @Immutable
    class IOSPlatform(ipad: Boolean): ComposePlatform(
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = !ipad,
        supportPip = false,
        applePlatform = true,
        appleEnvironment = true,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = ipad
    )
    @Immutable
    data object MacAppleSiliconPlatform: ComposePlatform(
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = true,
        hasBackgroundLocation = true,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = true,
        appleEnvironment = true,
        isMobileAppRunningOnDesktop = true,
        hasLargeScreen = true
    )
    @Immutable
    class DesktopPlatform(isApple: Boolean): ComposePlatform(
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = false,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = true
    )
    @Immutable
    class WebDesktopPlatform(isApple: Boolean): ComposePlatform(
        hasMouse = true,
        hasKeyboard = true,
        hasLocation = true,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = true,
        hasLargeScreen = true
    )
    @Immutable
    class WebMobilePlatform(isApple: Boolean): ComposePlatform(
        hasMouse = false,
        hasKeyboard = false,
        hasLocation = true,
        hasBackgroundLocation = false,
        mayHaveWatch = false,
        supportPip = false,
        applePlatform = false,
        appleEnvironment = isApple,
        isMobileAppRunningOnDesktop = false,
        hasLargeScreen = false
    )
}

expect val composePlatform: ComposePlatform