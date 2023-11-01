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

import com.loohp.hkbuseta.shared.LastLookupRoute;
import com.loohp.hkbuseta.utils.JSONSerializable;
import com.loohp.hkbuseta.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preferences implements JSONSerializable {

    public static Preferences deserialize(JSONObject json) throws JSONException {
        String language = json.optString("language");
        Map<Integer, FavouriteRouteStop> favouriteRouteStops = JsonUtils.toMap(json.optJSONObject("favouriteRouteStops"), Integer::parseInt, v -> FavouriteRouteStop.deserialize((JSONObject) v));
        List<LastLookupRoute> lastLookupRoutes = JsonUtils.mapToList(json.optJSONArray("lastLookupRoutes"), v -> LastLookupRoute.Companion.deserialize((JSONObject) v));
        Map<Integer, List<Integer>> etaTileConfigurations = json.has("etaTileConfigurations") ? JsonUtils.toMap(json.optJSONObject("etaTileConfigurations"), Integer::parseInt, v -> JsonUtils.toList((JSONArray) v, int.class)) : new HashMap<>();
        return new Preferences(language, favouriteRouteStops, lastLookupRoutes, etaTileConfigurations);
    }

    public static Preferences createDefault() {
        return new Preferences("zh", new HashMap<>(), new ArrayList<>(), new HashMap<>());
    }

    private String language;
    private final Map<Integer, FavouriteRouteStop> favouriteRouteStops;
    private final List<LastLookupRoute> lastLookupRoutes;
    private final Map<Integer, List<Integer>> etaTileConfigurations;

    public Preferences(String language, Map<Integer, FavouriteRouteStop> favouriteRouteStops, List<LastLookupRoute> lastLookupRoutes, Map<Integer, List<Integer>> etaTileConfigurations) {
        this.language = language;
        this.favouriteRouteStops = favouriteRouteStops;
        this.lastLookupRoutes = lastLookupRoutes;
        this.etaTileConfigurations = etaTileConfigurations;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<Integer, FavouriteRouteStop> getFavouriteRouteStops() {
        return favouriteRouteStops;
    }

    public List<LastLookupRoute> getLastLookupRoutes() {
        return lastLookupRoutes;
    }

    public Map<Integer, List<Integer>> getEtaTileConfigurations() {
        return etaTileConfigurations;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("language", language);
        json.put("favouriteRouteStops", JsonUtils.fromMap(favouriteRouteStops, FavouriteRouteStop::serialize));
        json.put("lastLookupRoutes", JsonUtils.fromStream(lastLookupRoutes.stream().map(LastLookupRoute::serialize)));
        json.put("etaTileConfigurations", JsonUtils.fromMap(etaTileConfigurations, JsonUtils::fromCollection));
        return json;
    }

}
