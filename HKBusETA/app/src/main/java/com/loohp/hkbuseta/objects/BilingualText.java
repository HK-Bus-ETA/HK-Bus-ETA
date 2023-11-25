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

package com.loohp.hkbuseta.objects;

import androidx.compose.runtime.Immutable;

import com.loohp.hkbuseta.utils.IOSerializable;
import com.loohp.hkbuseta.utils.DataIOUtilsKtKt;
import com.loohp.hkbuseta.utils.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import kotlin.text.Charsets;

@Immutable
public class BilingualText implements JSONSerializable, IOSerializable {

    public static final BilingualText EMPTY = new BilingualText("", "");

    public static BilingualText deserialize(JSONObject json) {
        String zh = json.optString("zh");
        String en = json.optString("en");
        return new BilingualText(zh, en);
    }

    public static BilingualText deserialize(InputStream inputStream) {
        DataInputStream in = new DataInputStream(inputStream);
        String zh = DataIOUtilsKtKt.readString(in, Charsets.UTF_8);
        String en = DataIOUtilsKtKt.readString(in, Charsets.UTF_8);
        return new BilingualText(zh, en);
    }

    private final String zh;
    private final String en;

    public BilingualText(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    public String get(String language) {
        return language.equals("en") ? en : zh;
    }

    public String getZh() {
        return zh;
    }

    public String getEn() {
        return en;
    }

    @Override
    public String toString() {
        return zh + " " + en;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("zh", zh);
        json.put("en", en);
        return json;
    }

    @Override
    public void serialize(OutputStream outputStream) throws IOException {
        DataOutputStream out = new DataOutputStream(outputStream);
        DataIOUtilsKtKt.writeString(out, zh, Charsets.UTF_8);
        DataIOUtilsKtKt.writeString(out, en, Charsets.UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BilingualText that = (BilingualText) o;
        return Objects.equals(zh, that.zh) && Objects.equals(en, that.en);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zh, en);
    }

}
