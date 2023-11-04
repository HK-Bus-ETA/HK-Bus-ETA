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
import com.loohp.hkbuseta.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Immutable
public class DataContainer implements JSONSerializable {

    public static DataContainer deserialize(JSONObject json) throws JSONException {
        DataSheet dataSheet = DataSheet.deserialize(json.optJSONObject("dataSheet"));
        Set<String> busRoute = JsonUtils.toSet(json.optJSONArray("busRoute"), String.class);
        Map<String, List<String>> mtrBusStopAlias = JsonUtils.toMap(json.optJSONObject("mtrBusStopAlias"), v -> JsonUtils.toList((JSONArray) v, String.class));
        return new DataContainer(dataSheet, busRoute, mtrBusStopAlias);
    }

    private final DataSheet dataSheet;
    private final Set<String> busRoute;
    private final Map<String, List<String>> mtrBusStopAlias;

    public DataContainer(DataSheet dataSheet, Set<String> busRoute, Map<String, List<String>> mtrBusStopAlias) {
        this.dataSheet = dataSheet;
        this.busRoute = busRoute;
        this.mtrBusStopAlias = mtrBusStopAlias;
    }

    public DataSheet getDataSheet() {
        return dataSheet;
    }

    public Set<String> getBusRoute() {
        return busRoute;
    }

    public Map<String, List<String>> getMtrBusStopAlias() {
        return mtrBusStopAlias;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("dataSheet", dataSheet.serialize());
        json.put("busRoute", JsonUtils.fromCollection(busRoute));
        json.put("mtrBusStopAlias", JsonUtils.fromMap(mtrBusStopAlias, JsonUtils::fromCollection));
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataContainer that = (DataContainer) o;
        return Objects.equals(dataSheet, that.dataSheet) && Objects.equals(busRoute, that.busRoute) && Objects.equals(mtrBusStopAlias, that.mtrBusStopAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSheet, busRoute, mtrBusStopAlias);
    }
}
