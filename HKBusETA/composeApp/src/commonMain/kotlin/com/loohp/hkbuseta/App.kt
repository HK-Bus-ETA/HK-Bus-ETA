package com.loohp.hkbuseta

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ToastTextState
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.hasWatchAppInstalled
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.newScreen
import com.loohp.hkbuseta.appcontext.newScreenGroup
import com.loohp.hkbuseta.appcontext.snackbar
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
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
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.theme.AppTheme
import kotlinx.coroutines.delay

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

            BackButtonEffect {
                if (historyStack.size > 1) {
                    historyStack.last().finish()
                } else {
                    exitApp()
                }
            }
            LaunchedEffect (toastState) {
                if (toastState.text.isNotBlank()) {
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = toastState.text,
                            duration = toastState.duration.snackbar,
                            actionLabel = toastState.actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            toastState.action?.invoke()
                        }
                    } finally {
                        ToastTextState.resetToastState()
                    }
                }
            }
            LaunchedEffect (instance) {
                instance.logFirebaseEvent("new_screen", AppBundle().apply {
                    putString("id", instance.screen.name.lowercase())
                })
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
                        AnimatedContent(
                            targetState = instance,
                            contentKey = { it.newScreenGroup() }
                        ) {
                            it.newScreen()
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