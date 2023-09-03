package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.JsonUtils
import com.loohp.hkbuseta.presentation.utils.StringUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt


class ListRouteActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = intent.extras!!.getString("result")?.let { JSONArray(it) } ?: throw RuntimeException()
        val result = JsonUtils.toList(list, JSONObject::class.java)
        setContent {
            ListRoutePage(this, result)
        }
    }
}

@Composable
fun ListRoutePage(instance: ListRouteActivity, result: List<JSONObject>) {
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
            MainElement(instance, result)
        }
    }
}

@Composable
fun MainElement(instance: ListRouteActivity, result: List<JSONObject>) {
    instance.setContentView(R.layout.route_list)
    val table: TableLayout = instance.findViewById(R.id.route_list)
    table.removeAllViews()
    for (route in result) {
        val color = when (route.optString("co")) {
            "kmb" -> 0xFFFF4747.toInt()
            "ctb" -> 0xFFFFE15E.toInt()
            "nlb" -> 0xFF9BFFC6.toInt()
            "mtr-bus" -> 0xFFAAD4FF.toInt()
            "gmb" -> 0xFFAAFFAF.toInt()
            else -> Color.WHITE
        }

        val tr = TableRow(instance)
        tr.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val selectableItemBackgroundResource = android.R.attr.selectableItemBackground
        val typedValue = TypedValue()
        if (instance.theme.resolveAttribute(
                selectableItemBackgroundResource,
                typedValue,
                true
            )
        ) {
            tr.setBackgroundResource(typedValue.resourceId)
        } else {
            tr.setBackgroundResource(android.R.drawable.list_selector_background)
        }

        val padding = (7.5 * instance.resources.displayMetrics.density).roundToInt()
        tr.setPadding(0, padding, 0, padding)
        tr.setOnClickListener {
            val intent = Intent(instance, StopsActivity::class.java)
            intent.putExtra("route", route.toString())
            instance.startActivity(intent)
        }
        val routeTextView = TextView(instance)
        tr.addView(routeTextView);
        routeTextView.text = route.optJSONObject("route").optString("route")
        routeTextView.setTextColor(color)
        routeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        val routeTextLayoutParams: ViewGroup.LayoutParams = routeTextView.layoutParams
        routeTextLayoutParams.width =
            (51 * instance.resources.displayMetrics.density).roundToInt()
        routeTextView.layoutParams = routeTextLayoutParams
        val destTextView = TextView(instance)
        tr.addView(destTextView);

        destTextView.isSingleLine = true
        destTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
        destTextView.marqueeRepeatLimit = -1
        destTextView.isFocusable = true
        destTextView.isFocusableInTouchMode = true
        destTextView.setHorizontallyScrolling(true)
        destTextView.isSelected = true
        val destTextLayoutParams: ViewGroup.LayoutParams = destTextView.layoutParams
        destTextLayoutParams.width =
            (95 * instance.resources.displayMetrics.density).roundToInt()
        destTextView.layoutParams = destTextLayoutParams

        var dest =
            route.optJSONObject("route").optJSONObject("dest").optString(Shared.language)
        if (Shared.language == "en") {
            dest = StringUtils.capitalize(dest)
        }
        destTextView.text = (if (Shared.language == "en") "To " else "往").plus(dest)
        destTextView.setTextColor(color)
        destTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        table.addView(tr)

        val scrollView: ScrollView = instance.findViewById(R.id.route_list_scroll)
        scrollView.requestFocus()
    }
}