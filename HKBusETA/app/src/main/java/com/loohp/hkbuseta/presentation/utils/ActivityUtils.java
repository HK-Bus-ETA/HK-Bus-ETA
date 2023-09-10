package com.loohp.hkbuseta.presentation.utils;

import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ActivityUtils {

    public static void startActivity(ComponentActivity activity, Intent intent, Consumer<ActivityResult> callback) {
        AtomicReference<ActivityResultLauncher<Intent>> ref = new AtomicReference<>();
        ActivityResultLauncher<Intent> launcher = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.StartActivityForResult(), result -> {
            callback.accept(result);
            ref.get().unregister();
        });
        ref.set(launcher);
        launcher.launch(intent);
    }

}
