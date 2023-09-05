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
import com.loohp.hkbuseta.presentation.utils.StringUtils
import java.util.Timer
import java.util.TimerTask


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

        setContent {
            LaunchedEffect (Unit) {
                Registry.getInstance(this@MainActivity)
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (Registry.getInstance(this@MainActivity).state == Registry.State.READY) {
                            cancel()
                            if (stopId == null || co == null || stop == null || route == null) {
                                startActivity(Intent(this@MainActivity, TitleActivity::class.java))
                            } else {
                                val intent = Intent(this@MainActivity, EtaActivity::class.java)
                                intent.putExtra("stopId", stopId)
                                intent.putExtra("co", co)
                                intent.putExtra("index", index)
                                intent.putExtra("stop", stop)
                                intent.putExtra("route", route)
                                startActivity(intent)
                            }
                            finish()
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Shared.LoadingLabel("zh", instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            Shared.LoadingLabel("en", instance)
        }
    }
}