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

import com.loohp.hkbuseta.utils.DistanceUtils;
import com.loohp.hkbuseta.utils.IOSerializable;
import com.loohp.hkbuseta.utils.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

@Immutable
public class Coordinates implements JSONSerializable, IOSerializable {

    public static Coordinates deserialize(JSONObject json) {
        double lat = json.optDouble("lat");
        double lng = json.optDouble("lng");
        return new Coordinates(lat, lng);
    }

    public static Coordinates deserialize(InputStream inputStream) throws IOException {
        DataInputStream in = new DataInputStream(inputStream);
        double lat = in.readDouble();
        double lng = in.readDouble();
        return new Coordinates(lat, lng);
    }

    public static Coordinates fromArray(double[] array) {
        return new Coordinates(array[0], array[1]);
    }

    private final double lat;
    private final double lng;

    public Coordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double distance(Coordinates other) {
        return DistanceUtils.findDistance(lat, lng, other.lat, other.lng);
    }

    public double[] toArray() {
        return new double[] {lat, lng};
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("lat", lat);
        json.put("lng", lng);
        return json;
    }

    @Override
    public void serialize(OutputStream outputStream) throws IOException {
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeDouble(lat);
        out.writeDouble(lng);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return Double.compare(that.lat, lat) == 0 && Double.compare(that.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lng);
    }

}
