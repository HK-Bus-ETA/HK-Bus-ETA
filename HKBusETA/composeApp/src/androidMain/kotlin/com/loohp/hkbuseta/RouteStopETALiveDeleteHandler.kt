package com.loohp.hkbuseta

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.loohp.hkbuseta.utils.RouteStopETALiveActivity

class RouteStopETALiveDeleteHandler: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        RouteStopETALiveActivity.clearCurrentSelectedStop()
    }

}