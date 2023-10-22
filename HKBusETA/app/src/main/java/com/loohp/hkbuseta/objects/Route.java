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

@Immutable
public class Route implements JSONSerializable {

    public static Route deserialize(JSONObject json) throws JSONException {
        String route = json.optString("route");
        Map<String, String> bound = JsonUtils.toMap(json.optJSONObject("bound"), v -> (String) v);
        List<String> co = JsonUtils.toList(json.optJSONArray("co"), String.class);
        String serviceType = json.optString("serviceType");
        String nlbId = json.optString("nlbId");
        String gtfsId = json.optString("gtfsId");
        boolean ctbIsCircular = json.optBoolean("ctbIsCircular");
        boolean kmbCtbJoint = json.optBoolean("kmbCtbJoint");
        BilingualText dest = BilingualText.deserialize(json.optJSONObject("dest"));
        BilingualText orig = BilingualText.deserialize(json.optJSONObject("orig"));
        Map<String, List<String>> stops = JsonUtils.toMap(json.optJSONObject("stops"), v -> JsonUtils.toList((JSONArray) v, String.class));
        return new Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, dest, orig, stops);
    }

    private final String route;
    private final Map<String, String> bound;
    private final List<String> co;
    private final String serviceType;
    private final String nlbId;
    private final String gtfsId;
    private final boolean ctbIsCircular;
    private final boolean kmbCtbJoint;
    private final BilingualText dest;
    private final BilingualText orig;
    private final Map<String, List<String>> stops;

    public Route(String route, Map<String, String> bound, List<String> co, String serviceType, String nlbId, String gtfsId, boolean ctbIsCircular, boolean kmbCtbJoint, BilingualText dest, BilingualText orig, Map<String, List<String>> stops) {
        this.route = route;
        this.bound = bound;
        this.co = co;
        this.serviceType = serviceType;
        this.nlbId = nlbId;
        this.gtfsId = gtfsId;
        this.ctbIsCircular = ctbIsCircular;
        this.kmbCtbJoint = kmbCtbJoint;
        this.dest = dest;
        this.orig = orig;
        this.stops = stops;
    }

    public String getRouteNumber() {
        return route;
    }

    public Map<String, String> getBound() {
        return bound;
    }

    public List<String> getCo() {
        return co;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getNlbId() {
        return nlbId;
    }

    public String getGtfsId() {
        return gtfsId;
    }

    public boolean isCtbIsCircular() {
        return ctbIsCircular;
    }

    public boolean isKmbCtbJoint() {
        return kmbCtbJoint;
    }

    public BilingualText getDest() {
        return dest;
    }

    public BilingualText getOrig() {
        return orig;
    }

    public Map<String, List<String>> getStops() {
        return stops;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("route", route);
        json.put("bound", JsonUtils.fromMap(bound, v -> v));
        json.put("co", JsonUtils.fromCollection(co));
        json.put("serviceType", serviceType);
        json.put("nlbId", nlbId);
        json.put("gtfsId", gtfsId);
        json.put("ctbIsCircular", ctbIsCircular);
        json.put("kmbCtbJoint", kmbCtbJoint);
        json.put("dest", dest.serialize());
        json.put("orig", orig.serialize());
        json.put("stops", JsonUtils.fromMap(stops, JsonUtils::fromCollection));
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route1 = (Route) o;
        return ctbIsCircular == route1.ctbIsCircular && kmbCtbJoint == route1.kmbCtbJoint && Objects.equals(route, route1.route) && Objects.equals(bound, route1.bound) && Objects.equals(co, route1.co) && Objects.equals(serviceType, route1.serviceType) && Objects.equals(nlbId, route1.nlbId) && Objects.equals(gtfsId, route1.gtfsId) && Objects.equals(dest, route1.dest) && Objects.equals(orig, route1.orig) && Objects.equals(stops, route1.stops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, dest, orig, stops);
    }
}
