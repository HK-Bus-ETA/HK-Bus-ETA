package com.loohp.hkbuseta.common.widget

import platform.CoreLocation.CLLocationManager


actual fun CLLocationManager.authorizedForWidgetUpdates(): Boolean = authorizedForWidgetUpdates