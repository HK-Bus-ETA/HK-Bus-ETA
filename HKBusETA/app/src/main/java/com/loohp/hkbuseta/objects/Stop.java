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
public class Stop implements JSONSerializable {

    public static Stop deserialize(JSONObject json) {
        Coordinates location = Coordinates.deserialize(json.optJSONObject("location"));
        BilingualText name = BilingualText.deserialize(json.optJSONObject("name"));
        BilingualText remark = json.has("remark") ? BilingualText.deserialize(json.optJSONObject("remark")) : null;
        String kmbBbiId = json.has("kmbBbiId") ? json.optString("kmbBbiId") : null;
        return new Stop(location, name, remark, kmbBbiId);
    }

    private final Coordinates location;
    private final BilingualText name;
    private final BilingualText remark;
    private final String kmbBbiId;

    public Stop(Coordinates location, BilingualText name, BilingualText remark, String kmbBbiId) {
        this.location = location;
        this.name = name;
        this.remark = remark;
        this.kmbBbiId = kmbBbiId;
    }

    public Coordinates getLocation() {
        return location;
    }

    public BilingualText getName() {
        return name;
    }

    public BilingualText getRemark() {
        return remark;
    }

    public String getKmbBbiId() {
        return kmbBbiId;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("location", location.serialize());
        json.put("name", name.serialize());
        if (remark != null) {
            json.put("remark", remark.serialize());
        }
        if (kmbBbiId != null) {
            json.put("kmbBbiId", kmbBbiId);
        }
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return Objects.equals(location, stop.location) && Objects.equals(name, stop.name) && Objects.equals(remark, stop.remark) && Objects.equals(kmbBbiId, stop.kmbBbiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name, remark, kmbBbiId);
    }
}
