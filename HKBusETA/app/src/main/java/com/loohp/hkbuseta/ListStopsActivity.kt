package com.loohp.hkbuseta

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import com.loohp.hkbuseta.shared.Registry.StopData
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.DistanceUtils
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.formatDecimalSeparator
import org.json.JSONObject
import kotlin.math.roundToInt


data class StopEntry(
    val stopIndex: Int,
    val indexTextView: TextView,
    val stopTextView: TextView,
    val tableRow: TableRow,
    val stopName: String,
    val stopData: StopData,
    val lat: Double,
    val lng: Double,
    var distance: Double = Double.MAX_VALUE
)

data class OriginData(
    val lat: Double,
    val lng: Double,
    val onlyInRange: Boolean = false
)


class ListStopsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }?: throw RuntimeException()
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
    val kmbCtbJoint = route.optJSONObject("route")!!.optBoolean("kmbCtbJoint", false)
    val routeNumber = route.optJSONObject("route")!!.optString("route")
    val co = route.optString("co")
    val bound = if (co.equals("nlb")) route.optJSONObject("route")!!.optString("nlbId") else route.optJSONObject("route")!!.optJSONObject("bound")!!.optString(co)
    val gtfsId = route.optJSONObject("route")!!.optString("gtfsId")

    val stopEntries: MutableList<StopEntry> = ArrayList()
    var randomTr: TableRow? = null
    var randomBaseline: View? = null

    val stopsList = Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gtfsId)
    val lowestServiceType = stopsList.minOf { it.serviceType }

    for ((index, entry) in stopsList.withIndex()) {
        val i = index + 1
        val stopId = entry.stopId
        val stop = entry.stop

        val color = Color.White.adjustBrightness(if (entry.serviceType == lowestServiceType) 1F else 0.65F).toArgb()

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
        tr.addView(indexTextView)
        indexTextView.text = i.toString().plus(". ")
        indexTextView.setTextColor(color)
        indexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, StringUtils.scaledSize(15F, instance))
        val layoutParams: ViewGroup.LayoutParams = indexTextView.layoutParams
        layoutParams.width = (30 * instance.resources.displayMetrics.density).roundToInt()
        indexTextView.layoutParams = layoutParams
        val stopTextView = TextView(instance)
        tr.addView(stopTextView)

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

        var stopStr = stop.optJSONObject("name")!!.optString(Shared.language)
        if (Shared.language == "en") {
            stopStr = StringUtils.capitalize(stopStr)
        }
        stopTextView.text = stopStr
        stopTextView.setTextColor(color)
        stopTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, StringUtils.scaledSize(15F, instance))
        table.addView(tr)

        tr.setOnClickListener {
            val intent = Intent(instance, EtaActivity::class.java)
            intent.putExtra("stopId", stopId)
            intent.putExtra("co", co)
            intent.putExtra("index", i)
            intent.putExtra("stop", stop.toString())
            intent.putExtra("route", route.optJSONObject("route")!!.toString())
            instance.startActivity(intent)
        }
        tr.setOnLongClickListener {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            instance.runOnUiThread {
                val text = "".plus(i).plus(". ").plus(stopStr)
                Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
            }
            return@setOnLongClickListener true
        }

        val baseline = View(instance)
        baseline.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1)
        baseline.setBackgroundColor(Color(0xFF333333).toArgb())
        table.addView(baseline)

        val location = stop.optJSONObject("location")!!
        stopEntries.add(StopEntry(i, indexTextView, stopTextView, tr, stopStr, entry, location.optDouble("lat"), location.optDouble("lng")))

        randomTr = tr
        randomBaseline = baseline
    }
    val scrollView: ScrollView = instance.findViewById(R.id.stop_scroll)
    scrollView.requestFocus()

    val scrollTask: (OriginData) -> Unit = { origin ->
        val closest = stopEntries.onEach {
            it.distance = DistanceUtils.findDistance(origin.lat, origin.lng, it.lat, it.lng)
        }.minBy { it.distance }
        if (randomTr != null && (!origin.onlyInRange || closest.distance <= 0.3)) {
            scrollView.post {
                val brightness = if (closest.stopData.serviceType == lowestServiceType) 1F else 0.65F
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
                }.adjustBrightness(brightness).toArgb()

                val indexSpanString = SpannableString(closest.indexTextView.text)
                indexSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, indexSpanString.length, 0)
                closest.indexTextView.text = indexSpanString
                closest.indexTextView.setTextColor(color)

                val stopSpanString = SpannableString(closest.stopTextView.text)
                stopSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, stopSpanString.length, 0)
                closest.stopTextView.text = stopSpanString
                closest.stopTextView.setTextColor(color)

                val interchangeSearch = route.optBoolean("interchangeSearch", false)
                closest.tableRow.setOnLongClickListener {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    instance.runOnUiThread {
                        val text = "".plus(closest.stopIndex).plus(". ").plus(closest.stopName).plus("\n")
                            .plus(if (interchangeSearch) (if (Shared.language == "en") "Interchange " else "轉乘") else (if (Shared.language == "en") "Nearby " else "附近"))
                            .plus((closest.distance * 1000).roundToInt().formatDecimalSeparator()).plus(if (Shared.language == "en") "m" else "米")
                        Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                    }
                    return@setOnLongClickListener true
                }

                if (kmbCtbJoint) {
                    val secondColor = Color(0xFFFFE15E).adjustBrightness(brightness).toArgb()

                    val colorAnimIndex = ObjectAnimator.ofInt(closest.indexTextView, "textColor", color, secondColor)
                    colorAnimIndex.setEvaluator(ArgbEvaluator())
                    colorAnimIndex.startDelay = 1500
                    colorAnimIndex.duration = 5000
                    colorAnimIndex.repeatCount = -1
                    colorAnimIndex.repeatMode = ValueAnimator.REVERSE

                    val colorAnimStop = ObjectAnimator.ofInt(closest.stopTextView, "textColor", color, secondColor)
                    colorAnimStop.setEvaluator(ArgbEvaluator())
                    colorAnimStop.startDelay = 1500
                    colorAnimStop.duration = 5000
                    colorAnimStop.repeatCount = -1
                    colorAnimStop.repeatMode = ValueAnimator.REVERSE

                    colorAnimIndex.start()
                    colorAnimStop.start()
                }

                val elementHeight = randomTr.height + randomBaseline!!.height
                val y = elementHeight * closest.stopIndex - scrollView.height / 2 + elementHeight / 2
                scrollView.smoothScrollTo(0, y)
            }
        }
    }

    if (route.has("origin")) {
        val origin = route.optJSONObject("origin")!!
        scrollTask.invoke(OriginData(origin.optDouble("lat"), origin.optDouble("lng")))
    } else {
        LocationUtils.checkLocationPermission(instance) {
            if (it) {
                val future = LocationUtils.getGPSLocation(instance)
                Thread {
                    try {
                        val locationResult = future.get()
                        if (locationResult.isSuccess) {
                            val location = locationResult.location
                            scrollTask.invoke(OriginData(location.latitude, location.longitude, true))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }
}