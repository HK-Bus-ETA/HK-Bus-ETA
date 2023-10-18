/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class ConnectionUtils {

    @SuppressLint("WrongConstant")
    public static ConnectionType getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                for (ConnectionType type : ConnectionType.VALUES) {
                    if (capabilities.hasTransport(type.getTransportType())) {
                        return type;
                    }
                }
            }
        }
        return ConnectionType.NONE;
    }

    public enum ConnectionType {

        CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR),
        WIFI(NetworkCapabilities.TRANSPORT_WIFI),
        BLUETOOTH(NetworkCapabilities.TRANSPORT_BLUETOOTH),
        ETHERNET(NetworkCapabilities.TRANSPORT_ETHERNET),
        VPN(NetworkCapabilities.TRANSPORT_VPN),
        @SuppressLint("InlinedApi")
        USB(NetworkCapabilities.TRANSPORT_USB),
        NONE(-1);

        private static final ConnectionType[] VALUES = values();

        private final int transportType;

        ConnectionType(int transportType) {
            this.transportType = transportType;
        }

        public int getTransportType() {
            return transportType;
        }

        public boolean hasConnection() {
            return transportType >= 0;
        }
    }

}
