package com.loohp.hkbuseta.presentation.shared

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
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
        fun LoadingLabel(language: String, instance: Context) {
            val text = remember { mutableStateOf(if (language == "en") "Loading..." else "載入中...") }

            LaunchedEffect (Unit) {
                while (true) {
                    delay(500)
                    if (Registry.getInstance(
                            instance
                        ).state == Registry.State.UPDATING
                    ) {
                        text.value = if (language == "en") "Updating...\nMight take several minutes" else "更新數據中...\n可能需要幾分鐘"
                    }
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = text.value
            )
        }

        var language = "zh"

        val favoriteRouteStops: Map<Int, JSONObject> = ConcurrentHashMap()

    }

}