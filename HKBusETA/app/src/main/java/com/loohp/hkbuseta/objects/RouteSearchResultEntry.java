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

import androidx.compose.runtime.Stable;

import com.loohp.hkbuseta.utils.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

@Stable
public class RouteSearchResultEntry implements JSONSerializable {

    public static RouteSearchResultEntry deserialize(JSONObject json) throws JSONException {
        String routeKey = json.optString("routeKey");
        Route route = json.has("route") ? Route.deserialize(json.optJSONObject("route")) : null;
        Operator co = Operator.valueOf(json.optString("co"));
        StopInfo stop = json.has("stop") ? StopInfo.deserialize(json.optJSONObject("stop")) : null;
        StopLocation origin = json.has("origin") ? StopLocation.deserialize(json.optJSONObject("origin")) : null;
        boolean isInterchangeSearch = json.optBoolean("isInterchangeSearch");
        return new RouteSearchResultEntry(routeKey, route, co, stop, origin, isInterchangeSearch);
    }

    private String routeKey;
    private Route route;
    private Operator co;
    private StopInfo stopInfo;
    private StopLocation origin;
    private boolean isInterchangeSearch;

    public RouteSearchResultEntry(String routeKey, Route route, Operator co) {
        this.routeKey = routeKey;
        this.route = route;
        this.co = co;
    }

    public RouteSearchResultEntry(String routeKey, Route route, Operator co, StopInfo stopInfo, StopLocation origin, boolean isInterchangeSearch) {
        this.routeKey = routeKey;
        this.route = route;
        this.co = co;
        this.stopInfo = stopInfo;
        this.origin = origin;
        this.isInterchangeSearch = isInterchangeSearch;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Operator getCo() {
        return co;
    }

    public void setCo(Operator co) {
        this.co = co;
    }

    public StopInfo getStopInfo() {
        return stopInfo;
    }

    public void setStopInfo(StopInfo stopInfo) {
        this.stopInfo = stopInfo;
    }

    public StopLocation getOrigin() {
        return origin;
    }

    public void setOrigin(StopLocation origin) {
        this.origin = origin;
    }

    public boolean isInterchangeSearch() {
        return isInterchangeSearch;
    }

    public void setInterchangeSearch(boolean interchangeSearch) {
        isInterchangeSearch = interchangeSearch;
    }

    public RouteSearchResultEntry deepClone() throws JSONException {
        return deserialize(serialize());
    }

    public void strip() {
        this.route = null;
        if (this.stopInfo != null) {
            this.stopInfo.strip();
        }
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("routeKey", routeKey);
        if (route != null) {
            json.put("route", route.serialize());
        }
        json.put("co", co.name());
        if (stopInfo != null) {
            json.put("stop", stopInfo.serialize());
        }
        if (origin != null) {
            json.put("origin", origin.serialize());
        }
        json.put("interchangeSearch", isInterchangeSearch);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteSearchResultEntry that = (RouteSearchResultEntry) o;
        return isInterchangeSearch == that.isInterchangeSearch && Objects.equals(routeKey, that.routeKey) && Objects.equals(route, that.route) && Objects.equals(co, that.co) && Objects.equals(stopInfo, that.stopInfo) && Objects.equals(origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeKey, route, co, stopInfo, origin, isInterchangeSearch);
    }

    @Stable
    public static class StopInfo implements JSONSerializable {

        public static StopInfo deserialize(JSONObject json) throws JSONException {
            String stopId = json.optString("stopId");
            Stop data = json.has("data") ? Stop.deserialize(json.optJSONObject("data")) : null;
            double distance = json.optDouble("distance");
            Operator co = Operator.valueOf(json.optString("co"));
            return new StopInfo(stopId, data, distance, co);
        }

        private final String stopId;
        private Stop data;
        private final double distance;
        private final Operator co;

        public StopInfo(String stopId, Stop data, double distance, Operator co) {
            this.stopId = stopId;
            this.data = data;
            this.distance = distance;
            this.co = co;
        }

        public String getStopId() {
            return stopId;
        }

        public void setData(Stop data) {
            this.data = data;
        }

        public Stop getData() {
            return data;
        }

        public double getDistance() {
            return distance;
        }

        public Operator getCo() {
            return co;
        }

        public void strip() {
            this.data = null;
        }

        @Override
        public JSONObject serialize() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("stopId", stopId);
            if (data != null) {
                json.put("data", data.serialize());
            }
            json.put("distance", distance);
            json.put("co", co.name());
            return json;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StopInfo stopInfo = (StopInfo) o;
            return Double.compare(stopInfo.distance, distance) == 0 && Objects.equals(stopId, stopInfo.stopId) && Objects.equals(data, stopInfo.data) && Objects.equals(co, stopInfo.co);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stopId, data, distance, co);
        }
    }

}
