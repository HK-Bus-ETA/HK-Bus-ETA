package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
import com.loohp.hkbuseta.shared.ExtendedDataHolder
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clampSp
import org.json.JSONObject
import kotlin.math.roundToInt


class ListRoutesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        val resultKey = intent.extras!!.getString("resultKey")!!
        @Suppress("UNCHECKED_CAST")
        val result = ExtendedDataHolder.get(resultKey)!!.getExtra("result") as List<JSONObject>
        setContent {
            ListRoutePage(this, result)
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
fun ListRoutePage(instance: ListRoutesActivity, result: List<JSONObject>) {
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
fun MainElement(instance: ListRoutesActivity, result: List<JSONObject>) {
    val haptic = LocalHapticFeedback.current

    instance.setContentView(R.layout.route_list)
    val table: TableLayout = instance.findViewById(R.id.route_list)
    table.removeAllViews()
    for (route in result) {
        val co = route.optString("co")
        val routeNumber = if (co == "mtr" && Shared.language != "en") {
            Shared.getMtrLineName(route.optJSONObject("route")!!.optString("route"))
        } else {
            route.optJSONObject("route")!!.optString("route")
        }
        val color = when (co) {
            "kmb" -> if (Shared.isLWBRoute(routeNumber)) Color(0xFFF26C33) else Color(0xFFFF4747)
            "ctb" -> Color(0xFFFFE15E)
            "nlb" -> Color(0xFF9BFFC6)
            "mtr-bus" -> Color(0xFFAAD4FF)
            "gmb" -> Color(0xFF36FF42)
            "lightRail" -> Color(0xFFD3A809)
            "mtr" -> {
                when (route.optJSONObject("route")!!.optString("route")) {
                    "AEL" -> Color(0xFF00888E)
                    "TCL" -> Color(0xFFF3982D)
                    "TML" -> Color(0xFF9C2E00)
                    "TKL" -> Color(0xFF7E3C93)
                    "EAL" -> Color(0xFF5EB7E8)
                    "SIL" -> Color(0xFFCBD300)
                    "TWL" -> Color(0xFFE60012)
                    "ISL" -> Color(0xFF0075C2)
                    "KTL" -> Color(0xFF00A040)
                    "DRL" -> Color(0xFFEB6EA5)
                    else -> Color.White
                }
            }
            else -> Color.White
        }.toArgb()

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

        val routeTextView = TextView(instance)
        tr.addView(routeTextView)

        routeTextView.text = routeNumber
        routeTextView.setTextColor(color)
        val routeTextLayoutParams: ViewGroup.LayoutParams = routeTextView.layoutParams
        if (co == "mtr" && Shared.language != "en") {
            routeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, clampSp(instance, StringUtils.scaledSize(16F, instance), dpMax = StringUtils.scaledSize(16F, instance)))
            routeTextLayoutParams.width = (StringUtils.scaledSize(60F, instance) * instance.resources.displayMetrics.density).roundToInt()
        } else {
            routeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, clampSp(instance, StringUtils.scaledSize(20F, instance), dpMax = StringUtils.scaledSize(20F, instance)))
            routeTextLayoutParams.width = (StringUtils.scaledSize(51F, instance) * instance.resources.displayMetrics.density).roundToInt()
        }
        routeTextView.layoutParams = routeTextLayoutParams
        val destTextView = TextView(instance)
        tr.addView(destTextView)

        destTextView.isSingleLine = true
        destTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
        destTextView.marqueeRepeatLimit = -1
        destTextView.isFocusable = true
        destTextView.isFocusableInTouchMode = true
        destTextView.setHorizontallyScrolling(true)
        destTextView.isSelected = true
        val destTextLayoutParams: ViewGroup.LayoutParams = destTextView.layoutParams
        if (co == "mtr" && Shared.language != "en") {
            destTextLayoutParams.width = (StringUtils.scaledSize(86F, instance) * instance.resources.displayMetrics.density).roundToInt()
        } else {
            destTextLayoutParams.width = (StringUtils.scaledSize(95F, instance) * instance.resources.displayMetrics.density).roundToInt()
        }
        destTextView.layoutParams = destTextLayoutParams

        var dest = route.optJSONObject("route")!!.optJSONObject("dest")!!.optString(Shared.language)
        if (Shared.language == "en") {
            dest = StringUtils.capitalize(dest)
        }
        dest = (if (Shared.language == "en") "To " else "往").plus(dest)
        destTextView.text = dest
        destTextView.setTextColor(color)
        if (co == "mtr" && Shared.language != "en") {
            destTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, clampSp(instance, StringUtils.scaledSize(14F, instance), dpMax = StringUtils.scaledSize(17F, instance)))
        } else {
            destTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, clampSp(instance, StringUtils.scaledSize(15F, instance), dpMax = StringUtils.scaledSize(18F, instance)))
        }
        table.addView(tr)

        val baseline = View(instance)
        baseline.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1)
        baseline.setBackgroundColor(Color(0xFF333333).toArgb())
        table.addView(baseline)

        tr.setOnClickListener {
            val intent = Intent(instance, ListStopsActivity::class.java)
            intent.putExtra("route", route.toString())
            instance.startActivity(intent)
        }
        tr.setOnLongClickListener {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            instance.runOnUiThread {
                val text = routeNumber.plus(" ").plus(dest).plus("\n(").plus(if (Shared.language == "en") {
                    when (route.optString("co")) {
                        "kmb" -> if (Shared.isLWBRoute(routeNumber)) "LWB" else "KMB"
                        "ctb" -> "CTB"
                        "nlb" -> "NLB"
                        "mtr-bus" -> "MTR-Bus"
                        "gmb" -> "GMB"
                        "lightRail" -> "LRT"
                        "mtr" -> "MTR"
                        else -> "???"
                    }
                } else {
                    when (route.optString("co")) {
                        "kmb" -> if (Shared.isLWBRoute(routeNumber)) "龍運" else "九巴"
                        "ctb" -> "城巴"
                        "nlb" -> "嶼巴"
                        "mtr-bus" -> "港鐵巴士"
                        "gmb" -> "專線小巴"
                        "lightRail" -> "輕鐵"
                        "mtr" -> "港鐵"
                        else -> "???"
                    }
                }).plus(")")
                Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
            }
            return@setOnLongClickListener true
        }
    }
    val scrollView: ScrollView = instance.findViewById(R.id.route_list_scroll)
    scrollView.requestFocus()
}