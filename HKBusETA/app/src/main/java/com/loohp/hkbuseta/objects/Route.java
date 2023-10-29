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
        Map<Operator, String> bound = JsonUtils.toMap(json.optJSONObject("bound"), Operator::valueOf, v -> (String) v);
        List<Operator> co = JsonUtils.mapToList(json.optJSONArray("co"), v -> Operator.valueOf((String) v));
        String serviceType = json.optString("serviceType");
        String nlbId = json.optString("nlbId");
        String gtfsId = json.optString("gtfsId");
        boolean ctbIsCircular = json.optBoolean("ctbIsCircular");
        boolean kmbCtbJoint = json.optBoolean("kmbCtbJoint");
        GMBRegion gmbRegion = json.has("gmbRegion") ? GMBRegion.valueOfOrNull(json.optString("gmbRegion")) : null;
        BilingualText lrtCircular = json.has("lrtCircular") ? BilingualText.deserialize(json.optJSONObject("lrtCircular")) : null;
        BilingualText dest = BilingualText.deserialize(json.optJSONObject("dest"));
        BilingualText orig = BilingualText.deserialize(json.optJSONObject("orig"));
        Map<Operator, List<String>> stops = JsonUtils.toMap(json.optJSONObject("stops"), Operator::valueOf, v -> JsonUtils.toList((JSONArray) v, String.class));
        return new Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, stops);
    }

    private final String route;
    private final Map<Operator, String> bound;
    private final List<Operator> co;
    private final String serviceType;
    private final String nlbId;
    private final String gtfsId;
    private final boolean ctbIsCircular;
    private final boolean kmbCtbJoint;
    private final GMBRegion gmbRegion;
    private final BilingualText lrtCircular;
    private final BilingualText dest;
    private final BilingualText orig;
    private final Map<Operator, List<String>> stops;

    public Route(String route, Map<Operator, String> bound, List<Operator> co, String serviceType, String nlbId, String gtfsId, boolean ctbIsCircular, boolean kmbCtbJoint, GMBRegion gmbRegion, BilingualText lrtCircular, BilingualText dest, BilingualText orig, Map<Operator, List<String>> stops) {
        this.route = route;
        this.bound = bound;
        this.co = co;
        this.serviceType = serviceType;
        this.nlbId = nlbId;
        this.gtfsId = gtfsId;
        this.ctbIsCircular = ctbIsCircular;
        this.kmbCtbJoint = kmbCtbJoint;
        this.gmbRegion = gmbRegion;
        this.lrtCircular = lrtCircular;
        this.dest = dest;
        this.orig = orig;
        this.stops = stops;
    }

    public String getRouteNumber() {
        return route;
    }

    public Map<Operator, String> getBound() {
        return bound;
    }

    public List<Operator> getCo() {
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

    public GMBRegion getGmbRegion() {
        return gmbRegion;
    }

    public BilingualText getLrtCircular() {
        return lrtCircular;
    }

    public BilingualText getDest() {
        return dest;
    }

    public BilingualText getOrig() {
        return orig;
    }

    public Map<Operator, List<String>> getStops() {
        return stops;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("route", route);
        json.put("bound", JsonUtils.fromMap(bound, v -> v));
        json.put("co", JsonUtils.fromStream(co.stream().map(Operator::name)));
        json.put("serviceType", serviceType);
        json.put("nlbId", nlbId);
        json.put("gtfsId", gtfsId);
        json.put("ctbIsCircular", ctbIsCircular);
        json.put("kmbCtbJoint", kmbCtbJoint);
        if (gmbRegion != null) {
            json.put("gmbRegion", gmbRegion.name());
        }
        if (lrtCircular != null) {
            json.put("lrtCircular", lrtCircular.serialize());
        }
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
        return ctbIsCircular == route1.ctbIsCircular && kmbCtbJoint == route1.kmbCtbJoint && Objects.equals(route, route1.route) && Objects.equals(bound, route1.bound) && Objects.equals(co, route1.co) && Objects.equals(serviceType, route1.serviceType) && Objects.equals(nlbId, route1.nlbId) && Objects.equals(gtfsId, route1.gtfsId) && gmbRegion == route1.gmbRegion && Objects.equals(lrtCircular, route1.lrtCircular) && Objects.equals(dest, route1.dest) && Objects.equals(orig, route1.orig) && Objects.equals(stops, route1.stops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, stops);
    }
}
