package com.loohp.hkbuseta.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.currentTimeMillis


@Stable
class SpamClickDetector(
    private val requiredClicks: Int,
    private val timeWindowMillis: Long
) {
    private var clickCount = 0
    private var firstClickTime = 0L

    fun onClick(clicksLeft: (Int) -> Unit): Boolean {
        val now = currentTimeMillis()

        if (now - firstClickTime > timeWindowMillis) {
            clickCount = 1
            firstClickTime = now
            return false
        }

        clickCount++
        clicksLeft.invoke(requiredClicks - clickCount)
        if (clickCount >= requiredClicks) {
            clickCount = 0
            firstClickTime = 0L
            return true
        }

        return false
    }
}

@Composable
fun rememberSpamClickDetector(
    requiredClicks: Int = 5,
    timeWindowMillis: Long = 2000
): SpamClickDetector {
    return remember {
        SpamClickDetector(
            requiredClicks = requiredClicks,
            timeWindowMillis = timeWindowMillis
        )
    }
}