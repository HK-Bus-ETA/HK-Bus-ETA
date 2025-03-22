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

package com.loohp.hkbuseta.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.ArrowBack
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.OpenInNew
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformCircularProgressIndicator
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformTopAppBar
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.utils.sp
import io.github.alexzhirkevich.cupertino.toUIColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL.Companion.URLWithString
import platform.Foundation.dataWithContentsOfURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.UIKit.UIView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PdfViewerInterface(title: String, url: String, instance: AppActiveContext) {
    val haptics = LocalHapticFeedback.current
    if (composePlatform.isMobileAppRunningOnDesktop) {
        LaunchedEffect (Unit) {
            instance.handleWebpages(url, false, haptics.common).invoke()
            instance.finish()
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PlatformTopAppBar(
                    title = {
                        AutoResizeText(
                            modifier = Modifier.userMarquee(),
                            overflow = TextOverflow.Ellipsis,
                            text = title,
                            fontSizeRange = FontSizeRange(
                                max = 22.dp.sp,
                                step = 0.5F.sp
                            ),
                            lineHeight = 1.1F.em,
                            maxLines = 2
                        )
                    },
                    navigationIcon = {
                        PlatformButton(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            onClick = { instance.finish() }
                        ) {
                            PlatformIcon(
                                modifier = Modifier.size(30.dp),
                                painter = PlatformIcons.AutoMirrored.Filled.ArrowBack,
                                tint = platformLocalContentColor,
                                contentDescription = if (Shared.language == "en") "Back" else "返回"
                            )
                        }
                    },
                    actions = {
                        PlatformButton(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            onClick = instance.handleWebpages(url, false, haptics.common)
                        ) {
                            PlatformIcon(
                                modifier = Modifier.size(30.dp),
                                painter = PlatformIcons.AutoMirrored.Filled.OpenInNew,
                                tint = platformLocalContentColor,
                                contentDescription = if (Shared.language == "en") "Open in Browser" else "在瀏覽器開啟"
                            )
                        }
                    }
                )
            },
            content = { padding ->
                var state by remember { mutableStateOf(PdfLoadState.LOADING) }

                LaunchedEffect (state) {
                    if (state == PdfLoadState.ERROR) {
                        instance.handleWebpages(url, false, haptics.common).invoke()
                        instance.finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val pdfView = remember { PDFView() }
                    val backgroundColor = platformBackgroundColor

                    LaunchedEffect (Unit) {
                        withContext(Dispatchers.Main) {
                            pdfView.backgroundColor = backgroundColor.toUIColor()
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val data = NSData.dataWithContentsOfURL(url = URLWithString(url)!!)!!
                                val pdfDocument = PDFDocument(data = data)
                                withContext(Dispatchers.Main) {
                                    pdfView.document = pdfDocument
                                    pdfView.autoScales = true
                                    pdfView.maxScaleFactor = 10.0
                                    pdfView.minScaleFactor = pdfView.scaleFactorForSizeToFit
                                    state = PdfLoadState.SUCCESS
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                state = PdfLoadState.ERROR
                            }
                        }
                    }

                    UIKitView(
                        factory = {
                            @Suppress("USELESS_CAST")
                            pdfView as UIView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        properties = UIKitInteropProperties(
                            interactionMode = UIKitInteropInteractionMode.NonCooperative,
                            isNativeAccessibilityEnabled = true
                        )
                    )

                    if (state == PdfLoadState.LOADING) {
                        PlatformCircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = Color(0xFFF9DE09),
                            strokeWidth = 3.dp,
                            trackColor = Color(0xFF797979),
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }
        )
    }
}

enum class PdfLoadState {
    LOADING, SUCCESS, ERROR
}