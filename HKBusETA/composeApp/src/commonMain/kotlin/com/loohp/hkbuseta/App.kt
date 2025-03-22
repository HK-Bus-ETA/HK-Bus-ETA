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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ToastTextState
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.hasWatchAppInstalled
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.newScreen
import com.loohp.hkbuseta.appcontext.screenGroup
import com.loohp.hkbuseta.appcontext.shouldConsumePlatformWindowInsetsOnRoot
import com.loohp.hkbuseta.appcontext.snackbar
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.globalWritingFilesCounterState
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.BackButtonEffect
import com.loohp.hkbuseta.compose.Dangerous
import com.loohp.hkbuseta.compose.LeftToRightLayout
import com.loohp.hkbuseta.compose.PlatformAlertDialog
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformScaffold
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.RightToLeftRow
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.theme.AppTheme
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.dp
import kotlinx.coroutines.delay

@Composable
expect fun Modifier.consumePlatformWindowInsets(): Modifier

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
            val toastState by ToastTextState.toastState.collectAsStateMultiplatform()
            val snackbarHostState = remember { SnackbarHostState() }

            val globalWritingFilesCounter by globalWritingFilesCounterState.collectAsStateMultiplatform()
            var globalWritingFiles by remember { mutableStateOf(false) }
            val globalWritingFilesIndicatorAlpha by animateFloatAsState(
                targetValue = if (globalWritingFiles) 1F else 0F,
                animationSpec = tween(300)
            )

            BackButtonEffect {
                if (historyStack.size > 1) {
                    historyStack.last().finish()
                } else {
                    exitApp()
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
                        Box(
                            modifier = Modifier.applyIf(instance.shouldConsumePlatformWindowInsetsOnRoot) { consumePlatformWindowInsets() }
                        ) {
                            AnimatedContent(
                                targetState = instance,
                                contentKey = { it.screenGroup }
                            ) {
                                it.newScreen()
                            }
                            if (globalWritingFilesIndicatorAlpha > 0F) {
                                RightToLeftRow(
                                    modifier = Modifier
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