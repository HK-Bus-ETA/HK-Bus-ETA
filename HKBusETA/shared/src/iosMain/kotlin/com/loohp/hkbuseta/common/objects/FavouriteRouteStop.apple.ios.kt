package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.applicationBaseAppContext


actual val appContextForWidget: AppContext get() = applicationBaseAppContext!!