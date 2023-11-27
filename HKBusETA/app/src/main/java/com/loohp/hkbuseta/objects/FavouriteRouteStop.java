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
public class FavouriteRouteStop implements JSONSerializable {

    public static FavouriteRouteStop deserialize(JSONObject json) throws JSONException {
        String stopId = json.optString("stopId");
        Operator co = Operator.valueOf(json.optString("co"));
        int index = json.optInt("index");
        Stop stop = Stop.deserialize(json.optJSONObject("stop"));
        Route route = Route.deserialize(json.optJSONObject("route"));
        FavouriteStopMode favouriteStopMode = FavouriteStopMode.valueOfOrDefault(json.optString("favouriteStopMode"));
        return new FavouriteRouteStop(stopId, co, index, stop, route, favouriteStopMode);
    }

    private final String stopId;
    private final Operator co;
    private final int index;
    private final Stop stop;
    private final Route route;
    private final FavouriteStopMode favouriteStopMode;

    public FavouriteRouteStop(String stopId, Operator co, int index, Stop stop, Route route, FavouriteStopMode favouriteStopMode) {
        this.stopId = stopId;
        this.co = co;
        this.index = index;
        this.stop = stop;
        this.route = route;
        this.favouriteStopMode = favouriteStopMode;
    }

    public String getStopId() {
        return stopId;
    }

    public Operator getCo() {
        return co;
    }

    public int getIndex() {
        return index;
    }

    public Stop getStop() {
        return stop;
    }

    public Route getRoute() {
        return route;
    }

    public FavouriteStopMode getFavouriteStopMode() {
        return favouriteStopMode;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("stopId", stopId);
        json.put("co", co.name());
        json.put("index", index);
        json.put("stop", stop.serialize());
        json.put("route", route.serialize());
        json.put("favouriteStopMode", favouriteStopMode.name());
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavouriteRouteStop that = (FavouriteRouteStop) o;
        return index == that.index && Objects.equals(stopId, that.stopId) && Objects.equals(co, that.co) && Objects.equals(stop, that.stop) && Objects.equals(route, that.route) && favouriteStopMode == that.favouriteStopMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopId, co, index, stop, route, favouriteStopMode);
    }
}
