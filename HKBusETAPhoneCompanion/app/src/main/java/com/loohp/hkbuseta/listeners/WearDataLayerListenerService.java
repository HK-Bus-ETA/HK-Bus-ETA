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

package com.loohp.hkbuseta.listeners;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.loohp.hkbuseta.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class WearDataLayerListenerService extends WearableListenerService {

    public static final String EXPORT_PREFERENCE_PATH = "/HKBusETA/ExportPreference";

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        super.onMessageReceived(event);
        String path = event.getPath();
        if (path.equals(EXPORT_PREFERENCE_PATH)) {
            try {
                String preference = new JSONObject(new String(event.getData())).toString(4);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("exportPreference", preference);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
