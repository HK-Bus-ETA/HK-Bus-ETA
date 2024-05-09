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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.appcontext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.app.DummyInterface
import com.loohp.hkbuseta.app.MainLoading
import com.loohp.hkbuseta.app.RouteDetailsInterface
import com.loohp.hkbuseta.app.RouteMapSearchInterface
import com.loohp.hkbuseta.app.TitleInterface
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.HapticFeedbackType
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlow
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlowList
import com.loohp.hkbuseta.common.utils.wrap
import com.loohp.hkbuseta.common.utils.wrapList
import kotlinx.coroutines.flow.MutableStateFlow


expect fun initialScreen(): AppActiveContextCompose
expect fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>)

object ScreenState {

    val hasInterruptElement: MutableNonNullStateFlow<Boolean> = MutableStateFlow(false).wrap()

}

object HistoryStack {

    val historyStack: MutableNonNullStateFlowList<AppActiveContextCompose> = MutableStateFlow(listOf(initialScreen())).wrapList()

    fun popHistoryStack() {
        val stack = historyStack.value.toMutableList()
        val last = stack.removeLastOrNull()
        if (stack.isEmpty()) {
            handleEmptyStack(stack)
        }
        historyStack.value = stack
        last?.completeFinishCallback()
    }

}

fun AppActiveContext.isOnStack(): Boolean {
    return HistoryStack.historyStack.value.contains(this)
}

fun AppActiveContext.isTopOfStack(): Boolean {
    return HistoryStack.historyStack.value.lastOrNull() == this
}

data class ToastTextData(val text: String, val duration: ToastDuration) {

    companion object {

        val RESET: ToastTextData = ToastTextData("", ToastDuration.SHORT)

    }

}

object ToastTextState {

    val toastState: MutableNonNullStateFlow<ToastTextData> = MutableStateFlow(ToastTextData.RESET).wrap()

    fun resetToastState() {
        toastState.value = ToastTextData.RESET
    }

}

interface AppContextCompose : AppContext {

    val screenWidthScale: Float

}

interface AppActiveContextCompose: AppContextCompose, AppActiveContext {

    val screen: AppScreen
    val data: Map<String, Any?>
    val flags: Set<AppIntentFlag>

    fun setStatusNavBarColor(status: Color? = null, nav: Color? = null)

    fun completeFinishCallback()

    fun readFileFromFileChooser(fileType: String, read: (String) -> Unit)

    fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit)

    fun shareUrl(url: String, title: String?)

    fun switchActivity(appIntent: AppIntent)

    fun finishSelfOnly()

}

expect val applicationAppContext: AppContextCompose

val AppContext.compose: AppContextCompose get() = this as AppContextCompose
val AppActiveContext.compose: AppActiveContextCompose get() = this as AppActiveContextCompose

@Composable
fun AppActiveContextCompose.newScreen() {
    when (screen) {
        AppScreen.MAIN -> MainLoading(this, stopId = null, co = null, index = null, stop = ImmutableState(null), route = ImmutableState(null), listStopRoute = ImmutableState(null), listStopScrollToStop = null, listStopShowEta = null, listStopIsAlightReminder = null, queryKey = null, queryRouteNumber = null, queryBound = null, queryCo = null, queryDest = null, queryGMBRegion = null, queryStop = null, queryStopIndex = 0, queryStopDirectLaunch = false)
        AppScreen.TITLE, AppScreen.LIST_ROUTES, AppScreen.FAV, AppScreen.FAV_ROUTE_LIST_VIEW, AppScreen.NEARBY, AppScreen.SEARCH, AppScreen.SETTINGS, AppScreen.RECENT -> TitleInterface(this)
        AppScreen.LIST_STOPS, AppScreen.ETA, AppScreen.ETA_MENU -> RouteDetailsInterface(this)
        AppScreen.DUMMY -> DummyInterface(this)
        AppScreen.SEARCH_TRAIN -> RouteMapSearchInterface(this)
        else -> MainLoading(this, stopId = null, co = null, index = null, stop = ImmutableState(null), route = ImmutableState(null), listStopRoute = ImmutableState(null), listStopScrollToStop = null, listStopShowEta = null, listStopIsAlightReminder = null, queryKey = null, queryRouteNumber = null, queryBound = null, queryCo = null, queryDest = null, queryGMBRegion = null, queryStop = null, queryStopIndex = 0, queryStopDirectLaunch = false)
    }
}

enum class AppScreenGroup {
    UNKNOWN, MAIN, TITLE, ROUTE_STOPS, DUMMY, SEARCH_TRAIN
}

fun AppActiveContextCompose.newScreenGroup(): AppScreenGroup {
    return when (screen) {
        AppScreen.MAIN -> AppScreenGroup.MAIN
        AppScreen.TITLE, AppScreen.LIST_ROUTES, AppScreen.FAV, AppScreen.FAV_ROUTE_LIST_VIEW, AppScreen.NEARBY, AppScreen.SEARCH, AppScreen.SETTINGS, AppScreen.RECENT -> AppScreenGroup.TITLE
        AppScreen.LIST_STOPS, AppScreen.ETA, AppScreen.ETA_MENU -> AppScreenGroup.ROUTE_STOPS
        AppScreen.DUMMY -> AppScreenGroup.DUMMY
        AppScreen.SEARCH_TRAIN -> AppScreenGroup.SEARCH_TRAIN
        else -> AppScreenGroup.UNKNOWN
    }
}

val Theme.isDarkMode: Boolean @Composable get() = when (this) {
    Theme.LIGHT -> false
    Theme.DARK -> true
    Theme.SYSTEM -> isSystemInDarkTheme()
}

val ToastDuration.snackbar: SnackbarDuration get() = when (this) {
    ToastDuration.SHORT -> SnackbarDuration.Short
    ToastDuration.LONG -> SnackbarDuration.Long
}

val androidx.compose.ui.hapticfeedback.HapticFeedbackType.common: HapticFeedbackType get() {
    return when (this) {
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress -> HapticFeedbackType.LongPress
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove -> HapticFeedbackType.TextHandleMove
        else -> HapticFeedbackType.LongPress
    }
}

val HapticFeedbackType.compose: androidx.compose.ui.hapticfeedback.HapticFeedbackType get() {
    return when (this) {
        HapticFeedbackType.LongPress -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
        HapticFeedbackType.TextHandleMove -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
        else -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
    }
}

val androidx.compose.ui.hapticfeedback.HapticFeedback.common: HapticFeedback get() {
    return object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            this@common.performHapticFeedback(hapticFeedbackType.compose)
        }
    }
}