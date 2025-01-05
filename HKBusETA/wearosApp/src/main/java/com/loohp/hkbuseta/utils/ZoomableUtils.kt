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