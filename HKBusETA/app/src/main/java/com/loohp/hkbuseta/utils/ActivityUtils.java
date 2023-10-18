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
