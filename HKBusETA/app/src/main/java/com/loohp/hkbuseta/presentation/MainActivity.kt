package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.JsonUtils
import com.loohp.hkbuseta.presentation.utils.StringUtils
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.streams.toList


class MainActivity : ComponentActivity() {

    companion object {
        private var mContext: MainActivity? = null

        fun getContext(): MainActivity? {
            return mContext
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        mContext = this

        val stopId = intent.extras?.getString("stopId")
        val co = intent.extras?.getString("co")
        val index = intent.extras?.getInt("index")
        val stop = intent.extras?.getString("stop")
        val route = intent.extras?.getString("route")

        val queryRouteNumber = intent.extras?.getString("r")
        val queryBound = intent.extras?.getString("b")
        val queryCo = intent.extras?.getString("c").orEmpty()
        val queryDest = intent.extras?.getString("d")
        val queryGtfsId = intent.extras?.getString("g")
        val queryStop = intent.extras?.getString("s")

        setContent {
            LaunchedEffect (Unit) {
                Registry.getInstance(this@MainActivity)
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (Registry.getInstance(this@MainActivity).state == Registry.State.READY) {
                            cancel()
                            if (stopId != null && co != null && stop != null && route != null) {
                                val intent = Intent(this@MainActivity, EtaActivity::class.java)
                                intent.putExtra("stopId", stopId)
                                intent.putExtra("co", co)
                                intent.putExtra("index", index)
                                intent.putExtra("stop", stop)
                                intent.putExtra("route", route)
                                startActivity(intent)
                            } else if (queryRouteNumber != null) {
                                startActivity(Intent(this@MainActivity, TitleActivity::class.java))

                                val result = Registry.getInstance(this@MainActivity).findRoutes(queryRouteNumber);
                                if (result != null && result.isNotEmpty()) {
                                    val intent = Intent(this@MainActivity, ListRoutesActivity::class.java)
                                    intent.putExtra("result", JsonUtils.fromCollection(result).toString())
                                    startActivity(intent)

                                    var results = result.stream().filter {
                                        return@filter when (queryCo) {
                                            "nlb" -> (queryCo.isEmpty() || it.optString("co") == queryCo) && (queryBound == null || it.optJSONObject("route")!!.optString("nlbId") == queryBound)
                                            "gmb" -> {
                                                val r = it.optJSONObject("route")!!
                                                (queryCo.isEmpty() || it.optString("co") == queryCo) && (queryBound == null || r.optJSONObject("bound")!!.optString(queryCo) == queryBound) && r.optString("gtfsId") == queryGtfsId
                                            }
                                            else -> (queryCo.isEmpty() || it.optString("co") == queryCo) && (queryBound == null || it.optJSONObject("route")!!.optJSONObject("bound")!!.optString(queryCo) == queryBound)
                                        }
                                    }.toList()
                                    if (queryDest != null) {
                                        val destFiltered = results.stream().filter {
                                            val dest = it.optJSONObject("route")!!.optJSONObject("dest")!!
                                            return@filter queryDest == dest.optString("zh") || queryDest == dest.optString("en")
                                        }.toList()
                                        if (destFiltered.isNotEmpty()) {
                                            results = destFiltered
                                        }
                                    }
                                    if (results.isNotEmpty()) {
                                        if (queryStop != null) {
                                            val it = results[0]
                                            val intent2 = Intent(this@MainActivity, ListStopsActivity::class.java)
                                            intent2.putExtra("route", it.toString())
                                            startActivity(intent2)

                                            val stops = Registry.getInstance(this@MainActivity).getAllStops(queryRouteNumber, queryBound, queryCo, queryGtfsId)
                                            var i = 1
                                            for (stopData in stops) {
                                                if (stopData.stopId == queryStop) {
                                                    val intent3 = Intent(this@MainActivity, EtaActivity::class.java)
                                                    intent3.putExtra("stopId", stopId)
                                                    intent3.putExtra("co", queryCo)
                                                    intent3.putExtra("index", i)
                                                    intent3.putExtra("stop", stopData.stop.toString())
                                                    intent3.putExtra("route", it.optJSONObject("route")!!.toString())
                                                    startActivity(intent3)
                                                    break
                                                }
                                                i++
                                            }
                                        } else if (results.size == 1) {
                                            val it = results[0]
                                            val intent2 = Intent(this@MainActivity, ListStopsActivity::class.java)
                                            intent2.putExtra("route", it.toString())
                                            startActivity(intent2)
                                        } else {
                                            val intent2 = Intent(this@MainActivity, ListRoutesActivity::class.java)
                                            intent2.putExtra("result", JsonUtils.fromCollection(results).toString())
                                            startActivity(intent2)
                                        }
                                    }
                                }
                                finishAffinity()
                            } else {
                                startActivity(Intent(this@MainActivity, TitleActivity::class.java))
                                finishAffinity()
                            }
                        } else if (Registry.getInstance(this@MainActivity).state == Registry.State.ERROR) {
                            cancel()
                            val intent = Intent(this@MainActivity, FatalErrorActivity::class.java)
                            intent.putExtra("zh", "發生錯誤\n請檢查您的網絡連接")
                            intent.putExtra("en", "Fatal Error\nPlease check your internet connection")
                            startActivity(intent)
                            finish()
                        }
                    }
                }, 0, 100)
            }
            Loading(this)
        }
    }
}

@Composable
fun Loading(instance: MainActivity) {
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
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Shared.LoadingLabel("zh", true, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
            Shared.LoadingLabel("en", false, instance)
        }
    }
}