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

package com.loohp.hkbuseta

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.AppActiveContextCompose
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.ComposePlatform
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ToastTextState
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.hasWatchAppInstalled
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.newScreen
import com.loohp.hkbuseta.appcontext.previous
import com.loohp.hkbuseta.appcontext.screenGroup
import com.loohp.hkbuseta.appcontext.shouldConsumePlatformWindowInsetsOnRoot
import com.loohp.hkbuseta.appcontext.snackbar
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.appcontext.globalWritingFilesCounterState
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.Dangerous
import com.loohp.hkbuseta.compose.LeftToRightLayout
import com.loohp.hkbuseta.compose.PlatformAlertDialog
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformScaffold
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.PredicativeBackGestureHandler
import com.loohp.hkbuseta.compose.RightToLeftRow
import com.loohp.hkbuseta.compose.allowRightSwipeBackGesture
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.theme.AppTheme
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
expect fun Modifier.platformSafeAreaSidesPaddingAndConsumeWindowInsets(): Modifier

expect fun exitApp()

expect fun watchDataOverwriteWarningInitialValue(): Boolean

@Composable
fun App(onReady: (() -> Unit)? = null) {
    LeftToRightLayout {
        var watchDataOverwriteWarning by remember { mutableStateOf(watchDataOverwriteWarningInitialValue()) }
        if (watchDataOverwriteWarning) {
            var requestSent by remember { mutableStateOf(false) }

            LaunchedEffect (Unit) {
                watchDataOverwriteWarning = watchDataOverwriteWarning && hasWatchAppInstalled(applicationAppContext).await()
                while (true) {
                    delay(1000)
                    watchDataOverwriteWarning = Registry.isNewInstall(applicationAppContext)
                }
            }
            LaunchedEffect (requestSent) {
                if (!requestSent) {
                    while (true) {
                        requestSent = applicationAppContext.requestPreferencesIfPossible().await()
                        delay(1000)
                    }
                }
            }

            NewLaunchDialog(
                icon = PlatformIcons.Filled.Dangerous,
                title = "重要提示" withEn "Important Warning",
                text = "為防止您的資料被覆蓋，請在手錶上開啟香港巴士到站預報程式。" withEn
                        "To prevent your data from being overwritten, please open your HK Bus ETA App on your Smartwatch."
            )
        } else {
            val historyStack by HistoryStack.historyStack.collectAsStateMultiplatform()
            val instance by remember { derivedStateOf { historyStack.last() } }
            val backInstance by remember { derivedStateOf { historyStack.previous() } }
            val toastState by ToastTextState.toastState.collectAsStateMultiplatform()
            val snackbarHostState = remember { SnackbarHostState() }

            val globalWritingFilesCounter by globalWritingFilesCounterState.collectAsStateMultiplatform()
            var globalWritingFiles by remember { mutableStateOf(false) }
            val globalWritingFilesIndicatorAlpha by animateFloatAsState(
                targetValue = if (globalWritingFiles) 1F else 0F,
                animationSpec = tween(300)
            )

            data class AnimateState(
                val target: Float,
                val animationSpec: AnimationSpec<Float>?,
                val callback: ((List<AppActiveContextCompose>) -> Unit)? = null,
            )

            val width = currentLocalWindowSize.width
            var backProgressState by remember { mutableStateOf(AnimateState(0F, null)) }
            val backProgressAnimation = remember { Animatable(0F, Float.VectorConverter, 0.01F) }
            val animatedBackProgress by remember { derivedStateOf { backProgressAnimation.value } }
            var predictive by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var shownExitToast by remember { mutableStateOf(false) }

            PredicativeBackGestureHandler { progress ->
                try {
                    var sign = 1
                    progress.collect { backEvent ->
                        sign = if (backEvent.swipeEdge == 1) -1 else 1
                        if (sign > 0 || allowRightSwipeBackGesture) {
                            backProgressState = AnimateState(backEvent.progress * sign, spring(stiffness = Spring.StiffnessHigh))
                        }
                    }
                    if (sign > 0 || allowRightSwipeBackGesture) {
                        val time = 220 * (1F - backProgressAnimation.value.absoluteValue)
                        backProgressState = AnimateState(1F * sign,  tween(time.roundToInt()))
                    }
                } catch (e: CancellationException) {
                    backProgressState = AnimateState(0F, spring(stiffness = Spring.StiffnessHigh))
                    throw e
                }
            }

            LaunchedEffect (backProgressAnimation.value) {
                if (backProgressAnimation.value.absoluteValue >= 1F) {
                    backProgressState = AnimateState(0F, null) {
                        if (it.size > 1) {
                            it.last().finish()
                        } else {
                            exitApp()
                        }
                    }
                    predictive = true
                }
            }

            LaunchedEffect (backProgressState) {
                val (target, spec, callback) = backProgressState
                scope.launch {
                    callback?.invoke(historyStack)
                    if (spec == null) {
                        backProgressAnimation.snapTo(target)
                    } else {
                        backProgressAnimation.animateTo(target, spec)
                    }
                }
            }

            LaunchedEffect (backInstance, animatedBackProgress) {
                if (backInstance == null && animatedBackProgress.absoluteValue > 0F) {
                    if (!shownExitToast) {
                        instance.showToastText(if (Shared.language == "en") {
                            "You are exiting the application"
                        } else {
                            "你正在退出應用程式"
                        }, ToastDuration.SHORT)
                        shownExitToast = true
                    }
                } else {
                    shownExitToast = false
                }
            }

            LaunchedEffect (toastState) {
                toastState?.apply {
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = text,
                            duration = duration.snackbar,
                            actionLabel = actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            action?.invoke()
                        }
                    } finally {
                        ToastTextState.resetToastState(id)
                    }
                }
            }

            LaunchedEffect (instance) {
                instance.logFirebaseEvent("new_screen", AppBundle().apply {
                    putString("id", instance.screen.name.lowercase())
                })
            }

            LaunchedEffect (globalWritingFilesCounter) {
                if (globalWritingFilesCounter > 0) {
                    delay(500)
                    globalWritingFiles = true
                } else {
                    globalWritingFiles = false
                }
            }

            AppTheme(
                useDarkTheme = Shared.theme.isDarkMode,
                customColor = Shared.color?.let { Color(it) }
            ) {
                LaunchedEffect (Unit) {
                    onReady?.invoke()
                }
                PlatformScaffold(
                    snackbarHost = {
                        SnackbarInterface(
                            instance = instance,
                            snackbarHostState = snackbarHostState
                        )
                    },
                    content = {
                        Box {
                            val screens = remember { mutableMapOf<AppScreenGroup, @Composable (AppActiveContextCompose) -> Unit>() }
                            AnimatedContent(
                                targetState = instance to backInstance,
                                contentKey = { (instance) -> instance.screenGroup },
                                transitionSpec = {
                                    if (!predictive) {
                                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                            .togetherWith(fadeOut(animationSpec = tween(90)))
                                    } else {
                                        EnterTransition.None.togetherWith(ExitTransition.None)
                                    }
                                }
                            ) { (instance, backInstance) ->
                                LaunchedEffect (instance.screenGroup) {
                                    predictive = false
                                }
                                LaunchedEffect (instance.screenGroup, backInstance?.screenGroup) {
                                    val itr = screens.keys.iterator()
                                    while (itr.hasNext()) {
                                        val screen = itr.next()
                                        if (screen != instance.screenGroup && screen != backInstance?.screenGroup) {
                                            itr.remove()
                                        }
                                    }
                                }
                                if (backInstance != null && animatedBackProgress.absoluteValue > 0F) {
                                    Box(
                                        modifier = Modifier
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(Color.Black.adjustAlpha(animatedBackProgress.absoluteValue * -0.1F + 0.1F))
                                            }
                                            .background(platformBackgroundColor)
                                            .applyIf(instance.shouldConsumePlatformWindowInsetsOnRoot) {
                                                platformSafeAreaSidesPaddingAndConsumeWindowInsets()
                                            }
                                    ) {
                                        screens.getOrPut(backInstance.screenGroup) { movableContentOf<AppActiveContextCompose> { it.newScreen() } }
                                            .invoke(backInstance)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            if (backInstance != null) {
                                                translationX = animatedBackProgress * width
                                                shape = RectangleShape
                                                shadowElevation = animatedBackProgress.absoluteValue * 10.dp.toPx()
                                            }
                                        }
                                        .drawWithContent {
                                            drawContent()
                                            if (backInstance != null) {
                                                drawRect(Color.Black.adjustAlpha(animatedBackProgress.absoluteValue * 0.1F))
                                            }
                                        }
                                        .background(platformBackgroundColor)
                                        .applyIf(instance.shouldConsumePlatformWindowInsetsOnRoot) {
                                            platformSafeAreaSidesPaddingAndConsumeWindowInsets()
                                        }
                                ) {
                                    screens.getOrPut(instance.screenGroup) { movableContentOf<AppActiveContextCompose> { it.newScreen() } }
                                        .invoke(instance)
                                }
                            }
                            if (globalWritingFilesIndicatorAlpha > 0F) {
                                RightToLeftRow(
                                    modifier = Modifier
                                        .applyIf(composePlatform is ComposePlatform.AndroidPlatform) { statusBarsPadding() }
                                        .align(Alignment.TopEnd)
                                        .background(platformBackgroundColor.adjustAlpha(0.5F), RoundedCornerShape(5.dp))
                                        .padding(5.dp)
                                        .graphicsLayer { alpha = globalWritingFilesIndicatorAlpha },
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(12F.sp.dp)
                                            .shadow(5.dp, CircleShape)
                                            .background(Color.Green, CircleShape),
                                    )
                                    PlatformText(
                                        textAlign = TextAlign.End,
                                        fontSize = 10F.sp,
                                        lineHeight = 10F.sp,
                                        maxLines = 2,
                                        text = if (Shared.language == "en") {
                                            "Saving App Data...\nPlease do not close the app"
                                        } else {
                                            "正在儲存資料...\n請勿關閉應用程式"
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
expect fun SnackbarInterface(
    instance: AppActiveContext,
    snackbarHostState: SnackbarHostState
)

@Composable
fun NewLaunchDialog(
    icon: Painter,
    title: BilingualText,
    text: BilingualText
) {
    PlatformAlertDialog(
        icon = {
            PlatformIcon(
                painter = icon,
                contentDescription = "${title["zh"]} ${title["en"]}"
            )
        },
        title = {
            PlatformText(text = "${title["zh"]}\n${title["en"]}", textAlign = TextAlign.Center)
        },
        text = {
            PlatformText(text = "${text["zh"]}\n${text["en"]}")
        },
        onDismissRequest = { /* do nothing */ },
        confirmButton = { /* do nothing */ },
        onConfirm = { /* do nothing */ }
    )
}