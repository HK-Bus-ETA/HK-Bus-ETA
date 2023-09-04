package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.StringUtils
import org.json.JSONObject
import kotlin.math.roundToInt


class StopsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) } ?: throw RuntimeException()
        setContent {
            StopsPage(this, route)
        }
    }
}

@Composable
fun StopsPage(instance: StopsActivity, route: JSONObject) {
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
fun MainElement(instance: StopsActivity, route: JSONObject) {
    instance.setContentView(R.layout.stop_list)
    val table: TableLayout = instance.findViewById(R.id.stop_list)
    table.removeAllViews()
    val routeNumber = route.optJSONObject("route").optString("route")
    val co = route.optString("co")
    val bound = if (co.equals("nlb")) route.optJSONObject("route").optString("nlbId") else route.optJSONObject("route").optJSONObject("bound").optString(co)
    var targetIndex = -1
    var targetDistance = 1.0
    var randomTr: TableRow? = null
    var i = 1
    for (entry in Registry.getInstance(instance).getAllStops(routeNumber, bound, co)) {
        val stopId = entry.key
        val stop = entry.value.first

        val color = if (entry.value.second.optString("serviceType") == "1") Color.WHITE else 0xFF999999.toInt()

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

        val padding = (7.5 * instance.resources.displayMetrics.density).roundToInt()
        tr.setPadding(0, padding, 0, padding)
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
        val indexTextView = TextView(instance)
        tr.addView(indexTextView);
        indexTextView.text = "".plus(i).plus(". ")
        indexTextView.setTextColor(color)
        indexTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
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
        destTextLayoutParams.width = (131 * instance.resources.displayMetrics.density).roundToInt()
        stopTextView.layoutParams = destTextLayoutParams

        var stopStr = stop.optJSONObject("name").optString(Shared.language)
        if (Shared.language == "en") {
            stopStr = StringUtils.capitalize(stopStr)
        }
        stopTextView.text = stopStr
        stopTextView.setTextColor(color)
        stopTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        table.addView(tr)

        if (route.has("origin")) {
            val origin = route.optJSONObject("origin")
            val location = stop.optJSONObject("location")
            val distance = Registry.getInstance(instance).findDistance(origin.optDouble("lat"), origin.optDouble("lng"), location.optDouble("lat"), location.optDouble("lng"))
            if (distance < targetDistance) {
                targetIndex = i
                targetDistance = distance
            }
        }
        randomTr = tr

        i++
    }
    val scrollView: ScrollView = instance.findViewById(R.id.stop_scroll)
    if (targetIndex >= 0 && randomTr != null) {
        scrollView.post {
            val y = randomTr.height * targetIndex - scrollView.height / 2 + randomTr.height / 2
            scrollView.scrollTo(0, y)
        }
    }
    scrollView.requestFocus()
}