package com.loohp.hkbuseta.presentation.shared

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
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
        fun LoadingLabel(language: String, instance: Context) {
            var state by remember { mutableStateOf(false) }

            LaunchedEffect (Unit) {
                while (true) {
                    delay(500)
                    if (Registry.getInstance(instance).state == Registry.State.UPDATING) {
                        state = true
                    }
                }
            }

            LoadingLabelText(state, language, instance)
        }

        @Composable
        private fun LoadingLabelText(updating: Boolean, language: String, instance: Context) {
            if (updating) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
                    text = if (language == "en") "Updating..." else "更新數據中..."
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(2.5F, instance), TextUnitType.Em),
                    text = if (language == "en") "Might take several minutes" else "可能需要幾分鐘"
                )
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
                    text = if (language == "en") "Loading..." else "載入中..."
                )
            }
        }

        var language = "zh"

        val favoriteRouteStops: Map<Int, JSONObject> = ConcurrentHashMap()

    }

}