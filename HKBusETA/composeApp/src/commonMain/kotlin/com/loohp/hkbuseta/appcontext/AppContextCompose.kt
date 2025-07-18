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

package com.loohp.hkbuseta.appcontext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.app.DummyInterface
import com.loohp.hkbuseta.app.MainLoading
import com.loohp.hkbuseta.app.PdfViewerInterface
import com.loohp.hkbuseta.app.RecentInterface
import com.loohp.hkbuseta.app.RouteDetailsInterface
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
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.StringReadChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random


expect fun initialScreen(): AppActiveContextCompose
expect fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>)

object ScreenState {

    val hasInterruptElement: MutableStateFlow<Boolean> = MutableStateFlow(false)

}

object HistoryStack {

    val historyStack: MutableStateFlow<List<AppActiveContextCompose>> = MutableStateFlow(listOf(element = initialScreen()))

    fun popHistoryStack() {
        val stack = historyStack.value.toMutableList()
        val last = stack.removeLastOrNull()
        if (stack.isEmpty()) {
            handleEmptyStack(stack)
        }
        historyStack.value = stack
        last?.completeFinishCallback()
    }

    fun clearAll() {
        historyStack.value = listOf(element = initialScreen())
    }

}

fun List<AppActiveContextCompose>.previous(): AppActiveContextCompose? {
    val currentKey = lastOrNull()?.screenGroup?: return null
    for (screen in asReversed().asSequence().drop(1)) {
        if (currentKey == AppScreenGroup.MAIN || screen.screenGroup != currentKey) {
            return screen
        }
    }
    return null
}

fun AppActiveContext.isOnStack(): Boolean {
    return HistoryStack.historyStack.value.contains(this)
}

fun AppActiveContext.isTopOfStack(): Boolean {
    return HistoryStack.historyStack.value.lastOrNull() == this
}

data class ToastTextData(
    val id: Int = Random.nextInt(),
    val text: String,
    val duration: ToastDuration,
    val actionLabel: String?,
    val action: (() -> Unit)?
)

object ToastTextState {

    val toastState: MutableStateFlow<ToastTextData?> = MutableStateFlow(null)

    fun resetToastState(id: Int) {
        if (toastState.value?.id == id) {
            toastState.value = null
        }
    }

}

interface AppContextCompose : AppContext {

    val screenWidthScale: Float

    override fun showToastText(text: String, duration: ToastDuration) {
        showToastText(text, duration, null, null)
    }

    fun showToastText(text: String, duration: ToastDuration, actionLabel: String?, action: (() -> Unit)?) {
        ToastTextState.toastState.value = ToastTextData(text = text, duration = duration, actionLabel = actionLabel, action = action)
    }

}

interface AppActiveContextCompose: AppContextCompose, AppActiveContext {

    val screen: AppScreen
    val data: MutableMap<String, Any?>
    val flags: Set<AppIntentFlag>

    fun setStatusNavBarColor(status: Color? = null, nav: Color? = null)

    fun completeFinishCallback()

    fun readFileFromFileChooser(fileType: String, read: suspend (StringReadChannel) -> Unit)

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
        AppScreen.TITLE, AppScreen.LIST_ROUTES, AppScreen.FAV, AppScreen.FAV_ROUTE_LIST_VIEW, AppScreen.NEARBY, AppScreen.SEARCH, AppScreen.SEARCH_TRAIN, AppScreen.SETTINGS -> TitleInterface(this)
        AppScreen.LIST_STOPS, AppScreen.ETA, AppScreen.ETA_MENU -> RouteDetailsInterface(this)
        AppScreen.DUMMY -> DummyInterface(this)
        AppScreen.RECENT -> RecentInterface(this)
        AppScreen.PDF -> PdfViewerInterface(this)
//        AppScreen.JOURNEY_PLANNER -> JourneyPlannerInterface(this)
        else -> MainLoading(this, stopId = null, co = null, index = null, stop = ImmutableState(null), route = ImmutableState(null), listStopRoute = ImmutableState(null), listStopScrollToStop = null, listStopShowEta = null, listStopIsAlightReminder = null, queryKey = null, queryRouteNumber = null, queryBound = null, queryCo = null, queryDest = null, queryGMBRegion = null, queryStop = null, queryStopIndex = 0, queryStopDirectLaunch = false)
    }
}

enum class AppScreenGroup {
    UNKNOWN, MAIN, TITLE, ROUTE_STOPS, DUMMY, RECENT, PDF, JOURNEY_PLANNER
}

val AppActiveContextCompose.screenGroup: AppScreenGroup get() {
    return when (screen) {
        AppScreen.MAIN -> AppScreenGroup.MAIN
        AppScreen.TITLE, AppScreen.LIST_ROUTES, AppScreen.FAV, AppScreen.FAV_ROUTE_LIST_VIEW, AppScreen.NEARBY, AppScreen.SEARCH, AppScreen.SEARCH_TRAIN, AppScreen.SETTINGS -> AppScreenGroup.TITLE
        AppScreen.LIST_STOPS, AppScreen.ETA, AppScreen.ETA_MENU -> AppScreenGroup.ROUTE_STOPS
        AppScreen.DUMMY -> AppScreenGroup.DUMMY
        AppScreen.RECENT -> AppScreenGroup.RECENT
        AppScreen.PDF -> AppScreenGroup.PDF
        AppScreen.JOURNEY_PLANNER -> AppScreenGroup.JOURNEY_PLANNER
        else -> AppScreenGroup.UNKNOWN
    }
}

val AppActiveContextCompose.shouldConsumePlatformWindowInsetsOnRoot: Boolean get() {
    return when (screenGroup) {
        AppScreenGroup.MAIN -> false
        else -> true
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