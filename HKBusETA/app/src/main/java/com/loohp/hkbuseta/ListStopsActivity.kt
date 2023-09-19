package com.loohp.hkbuseta

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.wear.compose.material.MaterialTheme
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.StringUtils
import org.json.JSONObject
import kotlin.math.roundToInt


class ListStopsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) } ?: throw RuntimeException()
        setContent {
            StopsPage(this, route)
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}

@Composable
fun StopsPage(instance: ListStopsActivity, route: JSONObject) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            Shared.MainTime()
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            MainElement(instance, route)
        }
    }
}

@Composable
fun MainElement(instance: ListStopsActivity, route: JSONObject) {
    val haptic = LocalHapticFeedback.current

    instance.setContentView(R.layout.stop_list)
    val table: TableLayout = instance.findViewById(R.id.stop_list)
    table.removeAllViews()
    val routeNumber = route.optJSONObject("route").optString("route")
    val co = route.optString("co")
    val bound = if (co.equals("nlb")) route.optJSONObject("route").optString("nlbId") else route.optJSONObject("route").optJSONObject("bound").optString(co)
    val gtfsId = route.optJSONObject("route").optString("gtfsId")
    var targetIndex = -1
    var targetIndexText: TextView? = null
    var targetStopText: TextView? = null
    var targetDistance = 1.0
    var randomTr: TableRow? = null
    var randomBaseline: View? = null
    var i = 1
    for (entry in Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gtfsId)) {
        val stopId = entry.stopId
        val stop = entry.stop

        val color = (if (entry.serviceType == 1) Color.White else Color(0xFF9E9E9E)).toArgb()

        val tr = TableRow(instance)
        tr.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val selectableItemBackgroundResource = android.R.attr.selectableItemBackground
        val typedValue = TypedValue()
        if (instance.theme.resolveAttribute(selectableItemBackgroundResource, typedValue, true)) {
            tr.setBackgroundResource(typedValue.resourceId)
        } else {
            tr.setBackgroundResource(android.R.drawable.list_selector_background)
        }

        val padding = (StringUtils.scaledSize(7.5F, instance) * instance.resources.displayMetrics.density).roundToInt()
        tr.setPadding(0, padding, 0, padding)

        val indexTextView = TextView(instance)
        tr.addView(indexTextView);
        indexTextView.text = "".plus(i).plus(". ")
        indexTextView.setTextColor(color)
        indexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, StringUtils.scaledSize(15F, instance))
        val layoutParams: ViewGroup.LayoutParams = indexTextView.layoutParams
        layoutParams.width = (30 * instance.resources.displayMetrics.density).roundToInt()
        indexTextView.layoutParams = layoutParams
        val stopTextView = TextView(instance)
        tr.addView(stopTextView);

        stopTextView.isSingleLine = true
        stopTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
        stopTextView.marqueeRepeatLimit = -1
        stopTextView.isFocusable = true
        stopTextView.isFocusableInTouchMode = true
        stopTextView.setHorizontallyScrolling(true)
        stopTextView.isSelected = true
        val destTextLayoutParams: ViewGroup.LayoutParams = stopTextView.layoutParams
        destTextLayoutParams.width = (StringUtils.scaledSize(120F, instance) * instance.resources.displayMetrics.density).roundToInt()
        stopTextView.layoutParams = destTextLayoutParams

        var stopStr = stop.optJSONObject("name").optString(Shared.language)
        if (Shared.language == "en") {
            stopStr = StringUtils.capitalize(stopStr)
        }
        stopTextView.text = stopStr
        stopTextView.setTextColor(color)
        stopTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, StringUtils.scaledSize(15F, instance))
        table.addView(tr)

        val stopIndex = i
        tr.setOnClickListener {
            val intent = Intent(instance, EtaActivity::class.java)
            intent.putExtra("stopId", stopId)
            intent.putExtra("co", co)
            intent.putExtra("index", stopIndex)
            intent.putExtra("stop", stop.toString())
            intent.putExtra("route", route.optJSONObject("route").toString())
            instance.startActivity(intent)
        }
        tr.setOnLongClickListener {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            instance.runOnUiThread {
                val text = "".plus(stopIndex).plus(". ").plus(stopStr)
                Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
            }
            return@setOnLongClickListener true
        }

        val baseline = View(instance)
        baseline.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1)
        baseline.setBackgroundColor(Color(0xFF333333).toArgb())
        table.addView(baseline)

        if (route.has("origin")) {
            val origin = route.optJSONObject("origin")
            val location = stop.optJSONObject("location")
            val distance = Registry.getInstance(instance).findDistance(origin.optDouble("lat"), origin.optDouble("lng"), location.optDouble("lat"), location.optDouble("lng"))
            if (distance < targetDistance) {
                targetIndex = i
                targetIndexText = indexTextView
                targetStopText = stopTextView
                targetDistance = distance
            }
        }
        randomTr = tr
        randomBaseline = baseline

        i++
    }
    val scrollView: ScrollView = instance.findViewById(R.id.stop_scroll)
    if (targetIndex >= 0 && randomTr != null) {
        scrollView.post {
            val indexSpanString = SpannableString(targetIndexText!!.text)
            indexSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, indexSpanString.length, 0)
            targetIndexText.text = indexSpanString

            val stopSpanString = SpannableString(targetStopText!!.text)
            stopSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, stopSpanString.length, 0)
            targetStopText.text = stopSpanString

            val elementHeight = randomTr.height + randomBaseline!!.height
            val y = elementHeight * targetIndex - scrollView.height / 2 + elementHeight / 2
            scrollView.scrollTo(0, y)
        }
    }
    scrollView.requestFocus()
}