package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class FatalErrorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zh = intent.extras?.getString("zh")
        val en = intent.extras?.getString("en")
        val exception = intent.extras?.getString("exception")

        Shared.invalidateCache(this)

        setContent {
            Message(this, zh, en, exception)
        }
    }
}

@Composable
fun Message(instance: FatalErrorActivity, zh: String?, en: String?, exception: String?) {
    HKBusETATheme {
        LaunchedEffect (Unit) {
            Registry.clearInstance()
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = StringUtils.scaledSize(if (zh == null) 14F else 16F, instance).dp.sp,
                color = MaterialTheme.colors.primary,
                text = zh ?: "發生錯誤"
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = StringUtils.scaledSize(if (en == null) 14F else 16F, instance).dp.sp,
                color = MaterialTheme.colors.primary,
                text = en ?: "Fatal Error Occurred"
            )
            if (zh == null && en == null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    fontSize = StringUtils.scaledSize(10F, instance).dp.sp,
                    color = MaterialTheme.colors.primary,
                    text = "巴士路線資料可能不完整\n點選重新載入刷新"
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    fontSize = StringUtils.scaledSize(10F, instance).dp.sp,
                    color = MaterialTheme.colors.primary,
                    text = "Bus route data might be corrupted\nClick reload to refresh"
                )
            }
            if (exception != null) {
                val focusRequester = remember { FocusRequester() }
                val scroll = rememberScrollState()
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current

                var scrollCounter by remember { mutableStateOf(0) }
                val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
                val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
                var scrollMoved by remember { mutableStateOf(false) }
                LaunchedEffect (scrollInProgress) {
                    if (scrollInProgress) {
                        scrollCounter++
                    }
                }
                LaunchedEffect (scrollCounter, scrollReachedEnd) {
                    delay(50)
                    if (scrollReachedEnd && scrollMoved) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    scrollMoved = true
                }

                LaunchedEffect (Unit) {
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.95F)
                        .fillMaxHeight(0.25F)
                        .verticalScrollWithScrollbar(
                            state = scroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 2.dp
                            )
                        )
                        .onRotaryScrollEvent {
                            scope.launch {
                                scroll.animateScrollBy(
                                    it.verticalScrollPixels / 2,
                                    TweenSpec(durationMillis = 500, easing = FastOutSlowInEasing)
                                )
                            }
                            true
                        }
                        .focusRequester(
                            focusRequester = focusRequester
                        )
                        .focusable()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Left,
                    fontSize = TextUnit(8F.dp.sp.value, TextUnitType.Sp),
                    color = Color(0xFF7C7C7C),
                    text = exception
                )
            }
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier
                        .width(StringUtils.scaledSize(50, instance).dp)
                        .height(StringUtils.scaledSize(50, instance).dp),
                    onClick = {
                        Shared.invalidateCache(instance)
                        val intent = Intent(instance, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        instance.startActivity(intent)
                        instance.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Icon(
                            modifier = Modifier.size(StringUtils.scaledSize(35F, instance).dp),
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = if (Shared.language == "en") "Relaunch App" else "重新載入",
                            tint = Color.Yellow,
                        )
                    }
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(25, instance).dp))
                Button(
                    modifier = Modifier
                        .width(StringUtils.scaledSize(50, instance).dp)
                        .height(StringUtils.scaledSize(50, instance).dp),
                    onClick = {
                        Shared.invalidateCache(instance)
                        instance.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Icon(
                            modifier = Modifier.size(StringUtils.scaledSize(35F, instance).dp),
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (Shared.language == "en") "Exit App" else "退出應用程式",
                            tint = Color.Red,
                        )
                    }
                )
            }
        }
    }
}