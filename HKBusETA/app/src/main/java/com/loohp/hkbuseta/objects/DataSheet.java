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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kotlin.Pair;

@Immutable
public class DataSheet implements JSONSerializable {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static DataSheet deserialize(JSONObject json) throws JSONException {
        List<LocalDate> holidays = JsonUtils.mapToList(json.optJSONArray("holidays"), v -> LocalDate.parse((String) v, DATE_FORMATTER));
        Map<String, Route> routeList = JsonUtils.toMap(json.optJSONObject("routeList"), v -> Route.deserialize((JSONObject) v));
        Map<String, Stop> stopList = JsonUtils.toMap(json.optJSONObject("stopList"), v -> Stop.deserialize((JSONObject) v));
        Map<String, List<Pair<Operator, String>>> stopMap = JsonUtils.toMap(json.optJSONObject("stopMap"), v -> JsonUtils.mapToList((JSONArray) v, vv -> {
            JSONArray array = (JSONArray) vv;
            return new Pair<>(Operator.valueOf(array.optString(0)), array.optString(1));
        }));
        return new DataSheet(holidays, routeList, stopList, stopMap);
    }

    public final List<LocalDate> holidays;
    public final Map<String, Route> routeList;
    public final Map<String, Stop> stopList;
    public final Map<String, List<Pair<Operator, String>>> stopMap;

    public DataSheet(List<LocalDate> holidays, Map<String, Route> routeList, Map<String, Stop> stopList, Map<String, List<Pair<Operator, String>>> stopMap) {
        this.holidays = holidays;
        this.routeList = routeList;
        this.stopList = stopList;
        this.stopMap = stopMap;
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public Map<String, Route> getRouteList() {
        return routeList;
    }

    public Map<String, Stop> getStopList() {
        return stopList;
    }

    public Map<String, List<Pair<Operator, String>>> getStopMap() {
        return stopMap;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("holidays", JsonUtils.fromStream(holidays.stream().map(DATE_FORMATTER::format)));
        json.put("routeList", JsonUtils.fromMap(routeList, Route::serialize));
        json.put("stopList", JsonUtils.fromMap(stopList, Stop::serialize));
        json.put("stopMap", JsonUtils.fromMap(stopMap, v -> JsonUtils.fromStream(v.stream().map(p -> JsonUtils.fromCollection(List.of(p.getFirst().name(), p.getSecond()))))));
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSheet dataSheet = (DataSheet) o;
        return Objects.equals(holidays, dataSheet.holidays) && Objects.equals(routeList, dataSheet.routeList) && Objects.equals(stopList, dataSheet.stopList) && Objects.equals(stopMap, dataSheet.stopMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holidays, routeList, stopList, stopMap);
    }
}
