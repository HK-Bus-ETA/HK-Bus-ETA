package com.loohp.hkbuseta.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType


fun Spanned.toAnnotatedString(context: Context, defaultTextSize: Float = 17F): AnnotatedString = buildAnnotatedString {
    append(this@toAnnotatedString.toString())
    this@toAnnotatedString.getSpans(0, this@toAnnotatedString.length, Any::class.java).forEach {
        val start = this@toAnnotatedString.getSpanStart(it)
        val end = this@toAnnotatedString.getSpanEnd(it)
        when (it) {
            is StyleSpan -> when (it.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
            }
            is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(it.foregroundColor)), start, end)
            is RelativeSizeSpan -> addStyle(SpanStyle(fontSize = TextUnit(defaultTextSize * it.sizeChange, TextUnitType.Sp)), start, end)
            is AbsoluteSizeSpan -> addStyle(SpanStyle(fontSize = TextUnit(if (it.dip) UnitUtils.dpToSp(context, UnitUtils.pixelsToDp(context, it.size.toFloat())) else UnitUtils.pixelsToDp(context, it.size.toFloat()), TextUnitType.Sp)), start, end)
        }
    }
}