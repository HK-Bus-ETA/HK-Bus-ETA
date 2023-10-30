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

import com.loohp.hkbuseta.utils.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

@Immutable
public class BilingualText implements JSONSerializable {

    public static final BilingualText EMPTY = new BilingualText("", "");

    public static BilingualText deserialize(JSONObject json) {
        String zh = json.optString("zh");
        String en = json.optString("en");
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
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("zh", zh);
        json.put("en", en);
        return json;
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
