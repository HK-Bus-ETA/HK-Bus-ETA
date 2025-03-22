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

package com.loohp.hkbuseta.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import coil3.request.ImageRequest
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import com.github.panpf.zoomimage.compose.zoom.ZoomableState
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import com.github.panpf.zoomimage.zoom.ScalesCalculator


fun ZoomableState.touchPointToRealPoint(
    touchPoint: Offset,
    realSize: Size,
): Offset {
    val (posX, posY) = touchPoint

    val (transformScaleX, transformScaleY) = transform.scale
    val (transformOffsetX, transformOffsetY) = transform.offset

    val (imageWidth, imageHeight) = contentSize
    val scaleX = realSize.width / imageWidth
    val scaleY = realSize.height / imageHeight

    val x = (posX - transformOffsetX) / transformScaleX * scaleX
    val y = (posY - transformOffsetY) / transformScaleY * scaleY

    return Offset(x, y)
}

fun ZoomableState.realPointToTouchPoint(
    realPoint: Offset,
    realSize: Size,
): Offset {
    val (posX, posY) = realPoint

    val (transformScaleX, transformScaleY) = transform.scale
    val (transformOffsetX, transformOffsetY) = transform.offset

    val (imageWidth, imageHeight) = contentSize
    val scaleX = realSize.width / imageWidth
    val scaleY = realSize.height / imageHeight

    val x = posX / scaleX * transformScaleX + transformOffsetX
    val y = posY / scaleY * transformScaleY + transformOffsetY

    return Offset(x, y)
}

fun ZoomableState.realPointToContentPoint(
    realPoint: Offset,
    realSize: Size,
): Offset {
    val (posX, posY) = realPoint

    val (imageWidth, imageHeight) = contentSize
    val scaleX = realSize.width / imageWidth
    val scaleY = realSize.height / imageHeight

    val x = posX / scaleX
    val y = posY / scaleY

    return Offset(x, y)
}

data class PredefinedScalesCalculator(
    val minScale: Float,
    val mediumScale: Float,
    val maxScale: Float
): ScalesCalculator {

    override fun calculate(
        containerSize: IntSizeCompat,
        contentSize: IntSizeCompat,
        contentOriginSize: IntSizeCompat,
        contentScale: ContentScaleCompat,
        minScale: Float,
        initialScale: Float
    ): ScalesCalculator.Result {
        return ScalesCalculator.Result(
            minScale = this.minScale,
            mediumScale = this.mediumScale,
            maxScale = this.maxScale
        )
    }

}

fun ScalesCalculator.Companion.predefined(minScale: Float, mediumScale: Float, maxScale: Float): PredefinedScalesCalculator {
    return PredefinedScalesCalculator(minScale, mediumScale, maxScale)
}

fun ImageRequest.Builder.unrestrictedBitmapSize(): ImageRequest. Builder {
    return maxBitmapSize(coil3.size.Size(Dimension.Undefined, Dimension.Undefined))
}