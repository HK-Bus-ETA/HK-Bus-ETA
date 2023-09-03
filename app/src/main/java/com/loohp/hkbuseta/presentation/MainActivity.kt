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
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
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
        mContext = this;
        setContent {
            LaunchedEffect (Unit) {
                Registry.INSTANCE
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (Registry.INSTANCE.state == Registry.State.READY) {
                            cancel()
                            startActivity(Intent(this@MainActivity, TitleActivity::class.java))
                            finish()
                        } else if (Registry.INSTANCE.state == Registry.State.ERROR) {
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
            Loading()
        }
    }
}

@Composable
fun Loading() {
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
            Shared.LoadingLabel("zh")
            Spacer(modifier = Modifier.size(10.dp))
            Shared.LoadingLabel("en")
        }
    }
}