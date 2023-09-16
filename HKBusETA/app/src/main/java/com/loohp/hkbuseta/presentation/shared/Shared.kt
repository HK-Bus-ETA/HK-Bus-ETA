package com.loohp.hkbuseta.presentation.shared

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.presentation.utils.StringUtils
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class Shared {

    companion object {

        @Composable
        fun MainTime() {
            TimeText(
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        fun LoadingLabel(language: String, includeImage: Boolean, includeProgress: Boolean, instance: Context) {
            var state by remember { mutableStateOf(false) }

            LaunchedEffect (Unit) {
                while (true) {
                    delay(500)
                    if (Registry.getInstance(instance).state == Registry.State.UPDATING) {
                        state = true
                    }
                }
            }

            LoadingLabelText(state, language, includeImage, includeProgress, instance)
        }

        @Composable
        private fun LoadingLabelText(updating: Boolean, language: String, includeImage: Boolean, includeProgress: Boolean, instance: Context) {
            if (updating) {
                var currentProgress by remember { mutableStateOf(0F) }
                val progressAnimation by animateFloatAsState(
                    targetValue = currentProgress,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "LoadingProgressAnimation"
                )
                if (includeProgress) {
                    LaunchedEffect (Unit) {
                        while (true) {
                            currentProgress = Registry.getInstance(instance).updatePercentage
                            delay(500)
                        }
                    }
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Updating..." else "更新數據中..."
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Might take a minute" else "可能需要約一分鐘"
                )
                if (includeProgress) {
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp, 0.dp),
                        color = Color(0xFFF9DE09),
                        progress = progressAnimation
                    )
                }
            } else {
                if (includeImage) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            modifier = Modifier
                                .size(StringUtils.scaledSize(50, instance).dp)
                                .align(Alignment.Center),
                            painter = painterResource(R.mipmap.icon_full_smaller),
                            contentDescription = instance.resources.getString(R.string.app_name)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Loading..." else "載入中..."
                )
            }
        }

        fun getMtrLineChineseName(lineName: String): String {
            return getMtrLineChineseName(lineName) { lineName }
        }

        fun getMtrLineChineseName(lineName: String, orElse: String): String {
            return getMtrLineChineseName(lineName) { orElse }
        }

        fun getMtrLineChineseName(lineName: String, orElse: () -> String): String {
            return when (lineName) {
                "AEL" -> "機場快綫"
                "TCL" -> "東涌綫"
                "TML" -> "屯馬綫"
                "TKL" -> "將軍澳綫"
                "EAL" -> "東鐵綫"
                "SIL" -> "南港島綫"
                "TWL" -> "荃灣綫"
                "ISL" -> "港島綫"
                "KTL" -> "觀塘綫"
                "DRL" -> "迪士尼綫"
                else -> orElse.invoke()
            }
        }

        var language = "zh"

        val favoriteRouteStops: Map<Int, JSONObject> = ConcurrentHashMap()

    }

}