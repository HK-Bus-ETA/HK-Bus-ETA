package com.loohp.hkbuseta.listeners;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.loohp.hkbuseta.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class WearDataLayerListenerService extends WearableListenerService {

    public static final String START_ACTIVITY_PATH = "/HKBusETA/Launch";

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        super.onMessageReceived(event);
        String path = event.getPath();
        if (path.equals(START_ACTIVITY_PATH)) {
            try {
                JSONObject data = new JSONObject(new String(event.getData()));
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                for (Iterator<String> itr = data.keys(); itr.hasNext(); ) {
                    String key = itr.next();
                    intent.putExtra(key, data.optString(key));
                }
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
