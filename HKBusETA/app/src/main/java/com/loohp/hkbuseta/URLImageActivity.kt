/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.UnitUtils
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable


@Stable
class URLImageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.extras!!.getString("url")!!

        setContent {
            ImageElement(url, this)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageElement(url: String, instance: URLImageActivity) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        var zoom by remember { mutableStateOf(false) }

        LaunchedEffect (Unit) {
            focusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
        ) {
            val zoomPadding by remember { derivedStateOf { if (zoom) 0F else UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getMinScreenSize(instance) * 0.15F) } }
            val animatedZoomPadding by animateFloatAsState(
                targetValue = zoomPadding,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "AnimatedZoomPadding"
            )
            var modifier = Modifier
                .fillMaxSize()
                .padding(animatedZoomPadding.dp)
            if (zoom) {
                modifier = modifier.zoomable(
                    state = rememberZoomableState(ZoomSpec(maxZoomFactor = 5F)),
                    onClick = {
                        zoom = !zoom
                    }
                )
            } else {
                modifier = modifier.combinedClickable(
                    onClick = {
                        zoom = !zoom
                    }
                )
            }
            SubcomposeAsyncImage(
                modifier = modifier,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .requiredSize(25.dp),
                        color = Color.LightGray,
                        strokeWidth = 3.dp,
                        trackColor = Color.DarkGray,
                        strokeCap = StrokeCap.Round,
                    )
                },
                model = ImageRequest.Builder(instance).size(1920).data(url).build(),
                contentDescription = "",
            )
        }
    }
}