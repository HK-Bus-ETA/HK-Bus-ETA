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

package com.loohp.hkbuseta.shared;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;

import androidx.compose.runtime.Immutable;
import androidx.compose.runtime.Stable;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileUpdateRequester;

import com.loohp.hkbuseta.R;
import com.loohp.hkbuseta.branchedlist.BranchedList;
import com.loohp.hkbuseta.objects.BilingualText;
import com.loohp.hkbuseta.objects.DataSheet;
import com.loohp.hkbuseta.objects.FavouriteRouteStop;
import com.loohp.hkbuseta.objects.Preferences;
import com.loohp.hkbuseta.objects.Route;
import com.loohp.hkbuseta.objects.RouteSearchResultEntry;
import com.loohp.hkbuseta.objects.Stop;
import com.loohp.hkbuseta.objects.StopLocation;
import com.loohp.hkbuseta.tiles.EtaTileServiceEight;
import com.loohp.hkbuseta.tiles.EtaTileServiceFive;
import com.loohp.hkbuseta.tiles.EtaTileServiceFour;
import com.loohp.hkbuseta.tiles.EtaTileServiceOne;
import com.loohp.hkbuseta.tiles.EtaTileServiceSeven;
import com.loohp.hkbuseta.tiles.EtaTileServiceSix;
import com.loohp.hkbuseta.tiles.EtaTileServiceThree;
import com.loohp.hkbuseta.tiles.EtaTileServiceTwo;
import com.loohp.hkbuseta.utils.CollectionsUtils;
import com.loohp.hkbuseta.utils.ConnectionUtils;
import com.loohp.hkbuseta.utils.DistanceUtils;
import com.loohp.hkbuseta.utils.HTTPRequestUtils;
import com.loohp.hkbuseta.utils.IntUtils;
import com.loohp.hkbuseta.utils.JsonUtils;
import com.loohp.hkbuseta.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Registry {

    private static Registry INSTANCE = null;

    public static synchronized Registry getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Registry(context);
        }
        return INSTANCE;
    }

    public static synchronized void clearInstance() {
        INSTANCE = null;
    }

    private static final String PREFERENCES_FILE_NAME = "preferences.json";
    private static final String CHECKSUM_FILE_NAME = "checksum.json";
    private static final String DATA_SHEET_FILE_NAME = "data_sheet.json";
    private static final String BUS_ROUTE_FILE_NAME = "bus_routes.json";
    private static final String MTR_BUS_STOP_ALIAS_FILE_NAME = "mtr_bus_stop_alias.json";

    public static void invalidateCache(Context context) {
        try {
            context.getApplicationContext().deleteFile(CHECKSUM_FILE_NAME);
        } catch (Throwable ignore) {}
    }

    private static Preferences PREFERENCES = null;
    private static DataSheet DATA_SHEET = null;
    private static Set<String> BUS_ROUTE = null;
    private static Map<String, List<String>> MTR_BUS_STOP_ALIAS = null;

    private static TyphoonInfo typhoonInfo = null;
    private static long typhoonInfoLastUpdated = Long.MIN_VALUE;

    private volatile State state = State.LOADING;
    private volatile float updatePercentage = 0F;
    private volatile boolean preferencesLoaded = false;
    private final Object preferenceWriteLock = new Object();
    private final AtomicLong lastUpdateCheck = new AtomicLong(Long.MIN_VALUE);

    private Registry(Context context) {
        try {
            ensureData(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public State getState() {
        return state;
    }

    public float getUpdatePercentage() {
        return updatePercentage;
    }

    public boolean isPreferencesLoaded() {
        return preferencesLoaded;
    }

    public void updateTileService(Context context) {
        updateTileService(1, context);
        updateTileService(2, context);
        updateTileService(3, context);
        updateTileService(4, context);
        updateTileService(5, context);
        updateTileService(6, context);
        updateTileService(7, context);
        updateTileService(8, context);
    }

    public void updateTileService(int favoriteIndex, Context context) {
        TileUpdateRequester updater = TileService.getUpdater(context);
        switch (favoriteIndex) {
            case 1: {
                updater.requestUpdate(EtaTileServiceOne.class);
                break;
            }
            case 2: {
                updater.requestUpdate(EtaTileServiceTwo.class);
                break;
            }
            case 3: {
                updater.requestUpdate(EtaTileServiceThree.class);
                break;
            }
            case 4: {
                updater.requestUpdate(EtaTileServiceFour.class);
                break;
            }
            case 5: {
                updater.requestUpdate(EtaTileServiceFive.class);
                break;
            }
            case 6: {
                updater.requestUpdate(EtaTileServiceSix.class);
                break;
            }
            case 7: {
                updater.requestUpdate(EtaTileServiceSeven.class);
                break;
            }
            case 8: {
                updater.requestUpdate(EtaTileServiceEight.class);
                break;
            }
        }
    }

    public void setLanguage(String language, Context context) {
        Shared.Companion.setLanguage(language);
        try {
            if (PREFERENCES == null) {
                PREFERENCES = Preferences.createDefault();
            }
            PREFERENCES.setLanguage(language);
            synchronized (preferenceWriteLock) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(PREFERENCES.serialize().toString());
                    pw.flush();
                }
            }
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasFavouriteRouteStop(int favoriteIndex) {
        return Shared.Companion.getFavoriteRouteStops().get(favoriteIndex) != null;
    }

    public boolean isFavouriteRouteStop(int favoriteIndex, String stopId, String co, int index, Stop stop, Route route) {
        FavouriteRouteStop favouriteRouteStop = Shared.Companion.getFavoriteRouteStops().get(favoriteIndex);
        if (favouriteRouteStop == null) {
            return false;
        }
        if (!stopId.equals(favouriteRouteStop.getStopId())) {
            return false;
        }
        if (!co.equals(favouriteRouteStop.getCo())) {
            return false;
        }
        if (index != favouriteRouteStop.getIndex()) {
            return false;
        }
        if (!stop.getName().getZh().equals(favouriteRouteStop.getStop().getName().getZh())) {
            return false;
        }
        return route.getRouteNumber().equals(favouriteRouteStop.getRoute().getRouteNumber());
    }

    public void clearFavouriteRouteStop(int favoriteIndex, Context context) {
        try {
            Shared.Companion.getFavoriteRouteStops().remove(favoriteIndex);
            if (PREFERENCES != null && !PREFERENCES.getFavouriteRouteStops().isEmpty()) {
                PREFERENCES.getFavouriteRouteStops().remove(favoriteIndex);
                synchronized (preferenceWriteLock) {
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(PREFERENCES.serialize().toString());
                        pw.flush();
                    }
                }
            }
            updateTileService(favoriteIndex, context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFavouriteRouteStop(int favoriteIndex, String stopId, String co, int index, Stop stop, Route route, Context context) {
        try {
            FavouriteRouteStop favouriteRouteStop = new FavouriteRouteStop(stopId, co, index, stop, route);
            Shared.Companion.getFavoriteRouteStops().put(favoriteIndex, favouriteRouteStop);
            if (PREFERENCES == null) {
                PREFERENCES = Preferences.createDefault();
            }
            PREFERENCES.getFavouriteRouteStops().put(favoriteIndex, favouriteRouteStop);
            synchronized (preferenceWriteLock) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(PREFERENCES.serialize().toString());
                    pw.flush();
                }
            }
            updateTileService(favoriteIndex, context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLastLookupRoute(String routeNumber, String co, String meta, Context context) {
        try {
            Shared.Companion.addLookupRoute(routeNumber, co, meta);
            List<LastLookupRoute> lastLookupRoutes = Shared.Companion.getLookupRoutes();
            if (PREFERENCES == null) {
                PREFERENCES = Preferences.createDefault();
            }
            PREFERENCES.getLastLookupRoutes().clear();
            PREFERENCES.getLastLookupRoutes().addAll(lastLookupRoutes);
            synchronized (preferenceWriteLock) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(PREFERENCES.serialize().toString());
                    pw.flush();
                }
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLastLookupRoutes(Context context) {
        try {
            Shared.Companion.clearLookupRoute();
            PREFERENCES.getLastLookupRoutes().clear();
            synchronized (preferenceWriteLock) {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(PREFERENCES.serialize().toString());
                    pw.flush();
                }
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureData(Context context) throws IOException {
        if (state == State.READY) {
            return;
        }
        if (PREFERENCES != null && DATA_SHEET != null && BUS_ROUTE != null && MTR_BUS_STOP_ALIAS != null) {
            return;
        }

        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8))) {
                PREFERENCES = Preferences.deserialize(new JSONObject(reader.lines().collect(Collectors.joining())));
                Shared.Companion.setLanguage(PREFERENCES.getLanguage());
                Shared.Companion.getFavoriteRouteStops().putAll(PREFERENCES.getFavouriteRouteStops());
                List<LastLookupRoute> lastLookupRoutes = PREFERENCES.getLastLookupRoutes();
                for (Iterator<LastLookupRoute> itr = lastLookupRoutes.iterator(); itr.hasNext();) {
                    LastLookupRoute lastLookupRoute = itr.next();
                    if (lastLookupRoute.isValid()) {
                        Shared.Companion.addLookupRoute(lastLookupRoute);
                    } else {
                        itr.remove();
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            PREFERENCES = Preferences.createDefault();
        }
        preferencesLoaded = true;

        checkUpdate(context);
    }

    public long getLastUpdateCheck() {
        return lastUpdateCheck.get();
    }

    public void checkUpdate(Context context) {
        state = State.LOADING;
        lastUpdateCheck.set(System.currentTimeMillis());
        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        new Thread(() -> {
            try {
                ConnectionUtils.ConnectionType connectionType = ConnectionUtils.getConnectionType(context);

                Supplier<String> checksumFetcher = () -> {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    new Thread(() -> {
                        try {
                            future.complete(HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/checksum.md5"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            future.complete(null);
                        }
                    }).start();
                    try {
                        return future.get(8, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                };

                boolean cached = false;
                String checksum = connectionType.hasConnection() ? checksumFetcher.get() : null;
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_SHEET_FILE_NAME) && files.contains(BUS_ROUTE_FILE_NAME) && files.contains(MTR_BUS_STOP_ALIAS_FILE_NAME)) {
                    if (checksum == null) {
                        cached = true;
                    } else {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(CHECKSUM_FILE_NAME), StandardCharsets.UTF_8))) {
                            String localChecksum = reader.readLine();
                            if (Objects.equals(localChecksum, checksum)) {
                                cached = true;
                            }
                        }
                    }
                }

                if (cached) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(DATA_SHEET_FILE_NAME), StandardCharsets.UTF_8))) {
                        DATA_SHEET = DataSheet.deserialize(new JSONObject(reader.lines().collect(Collectors.joining("\n"))));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(BUS_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        BUS_ROUTE = JsonUtils.toSet(new JSONArray(reader.lines().collect(Collectors.joining("\n"))), String.class);
                        if (BUS_ROUTE.isEmpty()) {
                            throw new RuntimeException();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(MTR_BUS_STOP_ALIAS_FILE_NAME), StandardCharsets.UTF_8))) {
                        MTR_BUS_STOP_ALIAS = JsonUtils.toMap(new JSONObject(reader.lines().collect(Collectors.joining("\n"))), v -> JsonUtils.toList((JSONArray) v, String.class));
                        if (MTR_BUS_STOP_ALIAS.isEmpty()) {
                            throw new RuntimeException();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    state = State.READY;
                    updateTileService(context);
                } else if (!connectionType.hasConnection()) {
                    state = State.ERROR;
                } else {
                    state = State.UPDATING;
                    updatePercentage = 0F;
                    float percentageOffset = Shared.Companion.getFavoriteRouteStops().isEmpty() ? 0.15F : 0F;

                    long length = IntUtils.parseOr(HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/size.gz.dat"), -1);
                    JSONObject data = HTTPRequestUtils.getJSONResponseWithPercentageCallback("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/data.json.gz", length, GZIPInputStream::new, p -> updatePercentage = p * 0.75F + percentageOffset);

                    MTR_BUS_STOP_ALIAS = JsonUtils.toMap(data.optJSONObject("mtrBusStopAlias"), v -> JsonUtils.toList((JSONArray) v, String.class));
                    DATA_SHEET = DataSheet.deserialize(data.optJSONObject("dataSheet"));
                    BUS_ROUTE = JsonUtils.toSet(data.optJSONArray("busRoute"), String.class);
                    updatePercentage = 0.75F + percentageOffset;

                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(MTR_BUS_STOP_ALIAS_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(JsonUtils.fromMap(MTR_BUS_STOP_ALIAS, JsonUtils::fromCollection).toString());
                        pw.flush();
                    }
                    updatePercentage = 0.775F + percentageOffset;
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(DATA_SHEET_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(DATA_SHEET.serialize().toString());
                        pw.flush();
                    }
                    updatePercentage = 0.8F + percentageOffset;
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(BUS_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(JsonUtils.fromCollection(BUS_ROUTE).toString());
                        pw.flush();
                    }
                    updatePercentage = 0.825F + percentageOffset;
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(CHECKSUM_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(checksum == null ? "" : checksum);
                        pw.flush();
                    }
                    updatePercentage = 0.85F + percentageOffset;

                    float localUpdatePercentage = updatePercentage;
                    float percentagePerFav = 0.15F / Shared.Companion.getFavoriteRouteStops().size();
                    List<Runnable> updatedFavouriteRouteTasks = new ArrayList<>();
                    for (Map.Entry<Integer, FavouriteRouteStop> entry : Shared.Companion.getFavoriteRouteStops().entrySet()) {
                        try {
                            int favouriteRouteIndex = entry.getKey();
                            FavouriteRouteStop favouriteRoute = entry.getValue();

                            Route oldRoute = favouriteRoute.getRoute();
                            String stopId = favouriteRoute.getStopId();
                            String co = favouriteRoute.getCo();

                            List<RouteSearchResultEntry> newRoutes = findRoutes(oldRoute.getRouteNumber(), true, r -> {
                                if (!r.getBound().containsKey(co)) {
                                    return false;
                                }
                                if (co.equals("gmb")) {
                                    if (!r.getGtfsId().equals(oldRoute.getGtfsId())) {
                                        return false;
                                    }
                                } else if (co.equals("nlb")) {
                                    return r.getNlbId().equals(oldRoute.getNlbId());
                                }
                                return r.getBound().get(co).equals(oldRoute.getBound().get(co));
                            });

                            if (newRoutes.isEmpty()) {
                                updatedFavouriteRouteTasks.add(() -> clearFavouriteRouteStop(favouriteRouteIndex, context));
                                continue;
                            }
                            RouteSearchResultEntry newRouteData = newRoutes.get(0);
                            Route newRoute = newRouteData.getRoute();
                            List<StopData> stopList = getAllStops(
                                    newRoute.getRouteNumber(),
                                    co.equals("nlb") ? newRoute.getNlbId() : newRoute.getBound().get(co),
                                    co,
                                    newRoute.getGtfsId()
                            );

                            String finalStopIdCompare = stopId;
                            int index = CollectionsUtils.indexOf(stopList, d -> d.stopId.equals(finalStopIdCompare)) + 1;
                            Stop stop;
                            StopData stopData;
                            if (index < 1) {
                                index = Math.max(1, Math.min(favouriteRoute.getIndex(), stopList.size()));
                                stopData = stopList.get(index - 1);
                                stopId = stopData.getStopId();
                            } else {
                                stopData = stopList.get(index - 1);
                            }
                            stop = stopList.get(index - 1).getStop();

                            String finalStopId = stopId;
                            int finalIndex = index;
                            updatedFavouriteRouteTasks.add(() -> setFavouriteRouteStop(favouriteRouteIndex, finalStopId, co, finalIndex, stop, stopData.getRoute(), context));
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        localUpdatePercentage += percentagePerFav;
                        updatePercentage = localUpdatePercentage;
                    }
                    updatedFavouriteRouteTasks.forEach(Runnable::run);
                    updatePercentage = 1F;

                    state = State.READY;
                    updateTileService(context);
                }
                updatePercentage = 1F;
            } catch (Exception e) {
                e.printStackTrace();
                state = State.ERROR;
            }
            if (state != State.READY) {
                state = State.ERROR;
            }
        }).start();
    }

    public Route findRouteByKey(String inputKey, String routeNumber) {
        Route exact = DATA_SHEET.getRouteList().get(inputKey);
        if (exact != null) {
            return exact;
        }
        inputKey = inputKey.toLowerCase();
        Route nearestRoute = null;
        int distance = Integer.MAX_VALUE;
        for (Map.Entry<String, Route> entry : DATA_SHEET.getRouteList().entrySet()) {
            String key = entry.getKey();
            Route route = entry.getValue();
            if (routeNumber == null || route.getRouteNumber().equalsIgnoreCase(routeNumber)) {
                int editDistance = StringUtils.editDistance(key.toLowerCase(), inputKey);
                if (editDistance < distance) {
                    nearestRoute = route;
                    distance = editDistance;
                }
            }
        }
        return nearestRoute;
    }

    public Stop getStopById(String stopId) {
        return DATA_SHEET.getStopList().get(stopId);
    }

    public PossibleNextCharResult getPossibleNextChar(String input) {
        Set<Character> result = new HashSet<>();
        boolean exactMatch = false;
        for (String routeNumber : BUS_ROUTE) {
            if (routeNumber.startsWith(input)) {
                if (routeNumber.length() > input.length()) {
                    result.add(routeNumber.charAt(input.length()));
                } else {
                    exactMatch = true;
                }
            }
        }
        return new PossibleNextCharResult(result, exactMatch);
    }

    @Immutable
    public static class PossibleNextCharResult {

        private final Set<Character> characters;
        private final boolean hasExactMatch;

        public PossibleNextCharResult(Set<Character> characters, boolean hasExactMatch) {
            this.characters = characters;
            this.hasExactMatch = hasExactMatch;
        }

        public Set<Character> getCharacters() {
            return characters;
        }

        public boolean hasExactMatch() {
            return hasExactMatch;
        }

    }

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact) {
        return findRoutes(input, exact, r -> true, (r, c) -> true);
    }

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact, Predicate<Route> predicate) {
        return findRoutes(input, exact, predicate, (r, c) -> true);
    }

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact, BiPredicate<Route, String> coPredicate) {
        return findRoutes(input, exact, r -> true, coPredicate);
    }

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact, Predicate<Route> predicate, BiPredicate<Route, String> coPredicate) {
        Predicate<String> routeMatcher = exact ? r -> r.equals(input) : r -> r.startsWith(input);
        Map<String, RouteSearchResultEntry> matchingRoutes = new HashMap<>();

        for (Map.Entry<String, Route> entry : DATA_SHEET.getRouteList().entrySet()) {
            String key = entry.getKey();
            Route data = entry.getValue();
            if (data.isCtbIsCircular()) {
                continue;
            }
            if (routeMatcher.test(data.getRouteNumber()) && predicate.test(data)) {
                String co;
                Map<String, String> bound = data.getBound();
                if (bound.containsKey("kmb")) {
                    co = "kmb";
                } else if (bound.containsKey("ctb")) {
                    co = "ctb";
                } else if (bound.containsKey("nlb")) {
                    co = "nlb";
                } else if (bound.containsKey("mtr-bus")) {
                    co = "mtr-bus";
                } else if (bound.containsKey("gmb")) {
                    co = "gmb";
                } else if (bound.containsKey("lightRail")) {
                    co = "lightRail";
                } else if (bound.containsKey("mtr")) {
                    co = "mtr";
                } else {
                    continue;
                }
                if (!coPredicate.test(data, co)) {
                    continue;
                }
                String key0 = data.getRouteNumber() + "," + co + "," + (co.equals("nlb") ? data.getNlbId() : data.getBound().get(co)) + (co.equals("gmb") ? ("," + data.getGtfsId().substring(0, 4)) : "");

                if (matchingRoutes.containsKey(key0)) {
                    try {
                        RouteSearchResultEntry existingMatchingRoute = matchingRoutes.get(key0);

                        int type = Integer.parseInt(data.getServiceType());
                        int matchingType = Integer.parseInt(existingMatchingRoute.getRoute().getServiceType());

                        if (type < matchingType) {
                            existingMatchingRoute.setRouteKey(key);
                            existingMatchingRoute.setRoute(data);
                            existingMatchingRoute.setCo(co);
                        } else if (type == matchingType) {
                            int gtfs = IntUtils.parseOr(data.getGtfsId(), Integer.MAX_VALUE);
                            int matchingGtfs = IntUtils.parseOr(existingMatchingRoute.getRoute().getGtfsId(), Integer.MAX_VALUE);
                            if (gtfs < matchingGtfs) {
                                existingMatchingRoute.setRouteKey(key);
                                existingMatchingRoute.setRoute(data);
                                existingMatchingRoute.setCo(co);
                            }
                        }
                    } catch (NumberFormatException ignore) {}
                } else {
                    matchingRoutes.put(key0, new RouteSearchResultEntry(key, data, co));
                }
            }
        }

        if (matchingRoutes.isEmpty()) {
            return null;
        }

        List<RouteSearchResultEntry> routes = new ArrayList<>(matchingRoutes.values());
        routes.sort((a, b) -> {
            Map<String, String> boundA = a.getRoute().getBound();
            Map<String, String> boundB = b.getRoute().getBound();
            String coAStr = boundA.containsKey("kmb") ? "kmb" : (boundA.containsKey("ctb") ? "ctb" : (boundA.containsKey("nlb") ? "nlb" : (boundA.containsKey("mtr-bus") ? "mtr-bus" : (boundA.containsKey("gmb") ? "gmb" : (boundA.containsKey("lightRail") ? "lightRail" : "mtr")))));
            String coBStr = boundB.containsKey("kmb") ? "kmb" : (boundB.containsKey("ctb") ? "ctb" : (boundB.containsKey("nlb") ? "nlb" : (boundB.containsKey("mtr-bus") ? "mtr-bus" : (boundB.containsKey("gmb") ? "gmb" : (boundB.containsKey("lightRail") ? "lightRail" : "mtr")))));
            int coA = coAStr.equals("kmb") ? 0 : (coAStr.equals("ctb") ? 1 : (coAStr.equals("nlb") ? 2 : (coAStr.equals("mtr-bus") ? 3 : (coAStr.equals("gmb") ? 4 : (coAStr.equals("lightRail") ? 5 : 6)))));
            int coB = coBStr.equals("kmb") ? 0 : (coBStr.equals("ctb") ? 1 : (coBStr.equals("nlb") ? 2 : (coBStr.equals("mtr-bus") ? 3 : (coBStr.equals("gmb") ? 4 : (coBStr.equals("lightRail") ? 5 : 6)))));
            if (coA != coB) {
                return coA - coB;
            }

            Route routeA = a.getRoute();
            Route routeB = b.getRoute();

            String routeNumberA = routeA.getRouteNumber();
            String routeNumberB = routeB.getRouteNumber();

            if (coA == 5 || coA == 6) {
                int lineDiff = Integer.compare(Shared.Companion.getMtrLineSortingIndex(routeNumberA), Shared.Companion.getMtrLineSortingIndex(routeNumberB));
                if (lineDiff != 0) {
                    return lineDiff;
                }
                return -boundA.get("mtr").compareTo(boundB.get("mtr"));
            }
            if (coA == 2) {
                return IntUtils.parseOrZero(routeA.getNlbId()) - IntUtils.parseOrZero(routeB.getNlbId());
            }
            if (coA == 4) {
                int gtfsDiff = IntUtils.parseOrZero(routeA.getGtfsId()) - IntUtils.parseOrZero(routeB.getGtfsId());
                if (gtfsDiff != 0) {
                    return gtfsDiff;
                }
            }
            int typeDiff = IntUtils.parseOrZero(routeA.getServiceType()) - IntUtils.parseOrZero(routeB.getServiceType());
            if (typeDiff == 0) {
                if (coA == 1) {
                    return 0;
                }
                return -boundA.get(coAStr).compareTo(boundB.get(coBStr));
            }
            return typeDiff;
        });
        return routes;
    }

    public NearbyRoutesResult getNearbyRoutes(double lat, double lng, Set<String> excludedRouteNumbers, boolean isInterchangeSearch) {
        StopLocation origin = new StopLocation(lat, lng);

        Map<String, Stop> stops = DATA_SHEET.getStopList();
        List<RouteSearchResultEntry.StopInfo> nearbyStops = new ArrayList<>();

        Stop closestStop = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Stop> stopEntry : stops.entrySet()) {
            String stopId = stopEntry.getKey();
            Stop entry = stopEntry.getValue();
            StopLocation location = entry.getLocation();
            double distance = DistanceUtils.findDistance(lat, lng, location.getLat(), location.getLng());

            if (distance < closestDistance) {
                closestStop = entry;
                closestDistance = distance;
            }

            if (distance <= 0.3) {
                String co;
                if (stopId.matches("^[0-9A-Z]{16}$")) {
                    co = "kmb";
                } else if (stopId.matches("^[0-9]{6}$")) {
                    co = "ctb";
                } else if (stopId.matches("^[0-9]{1,4}$")) {
                    co = "nlb";
                } else if (stopId.matches("^[A-Z]?[0-9]{1,3}[A-Z]?-[A-Z][0-9]{3}$")) {
                    co = "mtr-bus";
                } else if (stopId.matches("^[0-9]{8}$")) {
                    co = "gmb";
                } else if (stopId.matches("^LR[0-9]+$")) {
                    co = "lightRail";
                } else if (stopId.matches("^[A-Z]{3}$")) {
                    co = "mtr";
                } else {
                    continue;
                }

                nearbyStops.add(new RouteSearchResultEntry.StopInfo(stopId, entry, distance, co));
            }
        }

        Map<String, RouteSearchResultEntry> nearbyRoutes = new HashMap<>();

        for (RouteSearchResultEntry.StopInfo nearbyStop : nearbyStops) {
            String stopId = nearbyStop.getStopId();

            for (Map.Entry<String, Route> entry : DATA_SHEET.getRouteList().entrySet()) {
                String key = entry.getKey();
                Route data = entry.getValue();

                if (excludedRouteNumbers.contains(data.getRouteNumber())) {
                    continue;
                }
                if (data.isCtbIsCircular()) {
                    continue;
                }

                boolean isKmb = data.getBound().containsKey("kmb") && data.getStops().get("kmb").contains(stopId);
                boolean isCtb = data.getBound().containsKey("ctb") && data.getStops().get("ctb").contains(stopId);
                boolean isNlb = data.getBound().containsKey("nlb") && data.getStops().get("nlb").contains(stopId);
                boolean isMtrBus = data.getBound().containsKey("mtr-bus") && data.getStops().get("mtr-bus").contains(stopId);
                boolean isGmb = data.getBound().containsKey("gmb") && data.getStops().get("gmb").contains(stopId);
                boolean isLrt = data.getBound().containsKey("lightRail") && data.getStops().get("lightRail").contains(stopId);
                boolean isMtr = data.getBound().containsKey("mtr") && data.getStops().get("mtr").contains(stopId);

                if (isKmb || isCtb || isNlb || isMtrBus || isGmb || isLrt || isMtr) {
                    String co = isKmb ? "kmb" : (isCtb ? "ctb" : (isNlb ? "nlb" : (isMtrBus ? "mtr-bus" : (isGmb ? "gmb" : (isLrt ? "lightRail" : "mtr")))));
                    String key0 = data.getRouteNumber() + "," + co + "," + (co.equals("nlb") ? data.getNlbId() : data.getBound().get(co)) + (co.equals("gmb") ? ("," + data.getGtfsId().substring(0, 4)) : "");

                    if (nearbyRoutes.containsKey(key0)) {
                        RouteSearchResultEntry existingNearbyRoute = nearbyRoutes.get(key0);

                        if (existingNearbyRoute.getStopInfo().getDistance() > nearbyStop.getDistance()) {
                            try {
                                int type = Integer.parseInt(data.getServiceType());
                                int matchingType = Integer.parseInt(existingNearbyRoute.getRoute().getServiceType());

                                if (type < matchingType) {
                                    existingNearbyRoute.setRouteKey(key);
                                    existingNearbyRoute.setStopInfo(nearbyStop);
                                    existingNearbyRoute.setRoute(data);
                                    existingNearbyRoute.setCo(co);
                                    existingNearbyRoute.setOrigin(origin);
                                    existingNearbyRoute.setInterchangeSearch(isInterchangeSearch);
                                } else if (type == matchingType) {
                                    int gtfs = IntUtils.parseOr(data.getGtfsId(), Integer.MAX_VALUE);
                                    int matchingGtfs = IntUtils.parseOr(existingNearbyRoute.getRoute().getGtfsId(), Integer.MAX_VALUE);
                                    if (gtfs < matchingGtfs) {
                                        existingNearbyRoute.setRouteKey(key);
                                        existingNearbyRoute.setStopInfo(nearbyStop);
                                        existingNearbyRoute.setRoute(data);
                                        existingNearbyRoute.setCo(co);
                                        existingNearbyRoute.setOrigin(origin);
                                        existingNearbyRoute.setInterchangeSearch(isInterchangeSearch);
                                    }
                                }
                            } catch (NumberFormatException ignore) {
                                existingNearbyRoute.setRouteKey(key);
                                existingNearbyRoute.setStopInfo(nearbyStop);
                                existingNearbyRoute.setRoute(data);
                                existingNearbyRoute.setCo(co);
                                existingNearbyRoute.setOrigin(origin);
                                existingNearbyRoute.setInterchangeSearch(isInterchangeSearch);
                            }
                        }
                    } else {
                        nearbyRoutes.put(key0, new RouteSearchResultEntry(key, data, co, nearbyStop, origin, isInterchangeSearch));
                    }
                }
            }
        }

        if (nearbyRoutes.isEmpty()) {
            return new NearbyRoutesResult(Collections.emptyList(), closestStop, closestDistance, lat, lng);
        }

        List<RouteSearchResultEntry> routes = new ArrayList<>(nearbyRoutes.values());
        ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
        int hour = hongKongTime.getHour();
        boolean isNight = hour >= 1 && hour < 5;
        DayOfWeek weekday = hongKongTime.getDayOfWeek();
        LocalDate date = hongKongTime.toLocalDate();

        boolean isHoliday = weekday.equals(DayOfWeek.SATURDAY) || weekday.equals(DayOfWeek.SUNDAY) || DATA_SHEET.getHolidays().contains(date);

        routes.sort(Comparator.comparing((RouteSearchResultEntry a) -> {
            Route route = a.getRoute();
            String routeNumber = route.getRouteNumber();
            Map<String, String> bound = route.getBound();

            String pa = String.valueOf(routeNumber.charAt(0));
            String sa = String.valueOf(routeNumber.charAt(routeNumber.length() - 1));
            int na = IntUtils.parseOrZero(routeNumber.replaceAll("[^0-9]", ""));

            if (bound.containsKey("gmb")) {
                na += 1000;
            } else if (bound.containsKey("mtr")) {
                na += isInterchangeSearch ? -2000 : 2000;
            }
            if (pa.equals("N") || routeNumber.equals("270S") || routeNumber.equals("271S") || routeNumber.equals("293S") || routeNumber.equals("701S") || routeNumber.equals("796S")) {
                na -= (isNight ? 1 : -1) * 10000;
            }
            if (sa.equals("S") && !routeNumber.equals("89S") && !routeNumber.equals("796S")) {
                na += 3000;
            }
            if (!isHoliday && (pa.equals("R") || sa.equals("R"))) {
                na += 100000;
            }
            return na;
        }).thenComparing(a -> {
            return a.getRoute().getRouteNumber();
        }).thenComparing(a -> {
            return IntUtils.parseOrZero(a.getRoute().getServiceType());
        }).thenComparing(a -> {
            String co = a.getCo();
            return co.equals("kmb") ? 0 : (co.equals("ctb") ? 1 : (co.equals("nlb") ? 2 : (co.equals("mtr-bus") ? 3 : (co.equals("gmb") ? 4 : (co.equals("lightRail") ? 5 : 6)))));
        }).thenComparing(Comparator.comparing((RouteSearchResultEntry a) -> {
            Route route = a.getRoute();
            Map<String, String> bound = route.getBound();
            if (bound.containsKey("mtr")) {
                return Shared.Companion.getMtrLineSortingIndex(route.getRouteNumber());
            }
            return 10;
        }).reversed()));

        Set<String> addedKeys = new HashSet<>();
        List<RouteSearchResultEntry> distinctRoutes = new ArrayList<>();
        for (RouteSearchResultEntry value : routes) {
            if (addedKeys.add(value.getRouteKey())) {
                distinctRoutes.add(value);
            }
        }

        return new NearbyRoutesResult(distinctRoutes, closestStop, closestDistance, lat, lng);
    }

    @Immutable
    public static class NearbyRoutesResult {

        private final List<RouteSearchResultEntry> result;
        private final Stop closestStop;
        private final double closestDistance;
        private final double lat;
        private final double lng;

        public NearbyRoutesResult(List<RouteSearchResultEntry> result, Stop closestStop, double closestDistance, double lat, double lng) {
            this.result = result;
            this.closestStop = closestStop;
            this.closestDistance = closestDistance;
            this.lat = lat;
            this.lng = lng;
        }

        public List<RouteSearchResultEntry> getResult() {
            return result;
        }

        public Stop getClosestStop() {
            return closestStop;
        }

        public double getClosestDistance() {
            return closestDistance;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }
    }

    public List<StopData> getAllStops(String routeNumber, String bound, String co, String gtfsId) {
        try {
            List<Pair<BranchedList<String, StopData>, Integer>> lists = new ArrayList<>();
            for (Route route : DATA_SHEET.getRouteList().values()) {
                if (routeNumber.equals(route.getRouteNumber()) && route.getCo().contains(co)) {
                    boolean flag;
                    if (co.equals("nlb")) {
                        flag = bound.equals(route.getNlbId());
                    } else {
                        flag = bound.equals(route.getBound().get(co));
                        if (co.equals("gmb")) {
                            flag &= gtfsId.substring(0, 4).equals(route.getGtfsId().substring(0, 4));
                        }
                    }
                    if (flag) {
                        BranchedList<String, StopData> localStops = new BranchedList<>();
                        List<String> stops = route.getStops().get(co);
                        int serviceType = IntUtils.parseOr(route.getServiceType(), 1);
                        for (String stopId : stops) {
                            localStops.add(stopId, new StopData(stopId, serviceType, DATA_SHEET.getStopList().get(stopId), route));
                        }
                        lists.add(Pair.create(localStops, serviceType));
                    }
                }
            }
            lists.sort(Comparator.comparing(p -> p.second));
            BranchedList<String, StopData> result = new BranchedList<>((a, b) -> {
                int aType = a.getServiceType();
                int bType = b.getServiceType();
                if (aType == bType) {
                    int aGtfs = IntUtils.parseOr(a.getRoute().getGtfsId(), Integer.MAX_VALUE);
                    int bGtfs = IntUtils.parseOr(b.getRoute().getGtfsId(), Integer.MAX_VALUE);
                    return aGtfs > bGtfs ? b : a;
                }
                return aType > bType ? b : a;
            });
            for (Pair<BranchedList<String, StopData>, Integer> pair : lists) {
                result.merge(pair.first);
            }
            return result.valuesWithBranchIds().stream().map(p -> p.first.withBranchIndex(p.second)).collect(Collectors.toList());
        } catch (Throwable e) {
            throw new RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gtfsId + ": " + e.getMessage(), e);
        }
    }

    public static class StopData {

        private final String stopId;
        private final int serviceType;
        private final Stop stop;
        private final Route route;
        private final Set<Integer> branchIds;

        private StopData(String stopId, int serviceType, Stop stop, Route route) {
            this(stopId, serviceType, stop, route, Collections.emptySet());
        }

        public StopData(String stopId, int serviceType, Stop stop, Route route, Set<Integer> branchIds) {
            this.stopId = stopId;
            this.serviceType = serviceType;
            this.stop = stop;
            this.route = route;
            this.branchIds = branchIds;
        }

        public String getStopId() {
            return stopId;
        }

        public int getServiceType() {
            return serviceType;
        }

        public Stop getStop() {
            return stop;
        }

        public Route getRoute() {
            return route;
        }

        public Set<Integer> getBranchIds() {
            return branchIds;
        }

        public StopData withBranchIndex(Set<Integer> branchIds) {
            return new StopData(stopId, serviceType, stop, route, branchIds);
        }
    }

    public Pair<List<BilingualText>, List<BilingualText>> getAllOriginsAndDestinations(String routeNumber, String bound, String co, String gtfsId) {
        try {
            List<Pair<BilingualText, Integer>> origs = new ArrayList<>();
            List<Pair<BilingualText, Integer>> dests = new ArrayList<>();
            for (Route route : DATA_SHEET.getRouteList().values()) {
                if (routeNumber.equals(route.getRouteNumber()) && route.getCo().contains(co)) {
                    boolean flag;
                    if (co.equals("nlb")) {
                        flag = bound.equals(route.getNlbId());
                    } else {
                        flag = bound.equals(route.getBound().get(co));
                        if (co.equals("gmb")) {
                            flag &= gtfsId.equals(route.getGtfsId());
                        }
                    }
                    if (flag) {
                        int serviceType = IntUtils.parseOr(route.getServiceType(), 1);

                        BilingualText orig = route.getOrig();
                        Pair<BilingualText, Integer> oldOrig = origs.stream().filter(d -> d.first.getZh().equals(orig.getZh())).findFirst().orElse(null);
                        if (oldOrig == null || oldOrig.second > serviceType) {
                            origs.add(Pair.create(orig, serviceType));
                        }

                        BilingualText dest = route.getDest();
                        Pair<BilingualText, Integer> oldDest = dests.stream().filter(d -> d.first.getZh().equals(dest.getZh())).findFirst().orElse(null);
                        if (oldDest == null || oldDest.second > serviceType) {
                            dests.add(Pair.create(dest, serviceType));
                        }
                    }
                }
            }
            return Pair.create(
                    origs.stream().sorted(Comparator.comparing(p -> p.second)).map(p -> p.first).collect(Collectors.toList()),
                    dests.stream().sorted(Comparator.comparing(p -> p.second)).map(p -> p.first).collect(Collectors.toList())
            );
        } catch (Throwable e) {
            throw new RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gtfsId + ": " + e.getMessage(), e);
        }
    }

    public BilingualText getStopSpecialDestinations(String stopId, String co, Route route) {
        String bound = route.getBound().get(co);
        switch (stopId) {
            case "LHP": {
                if (bound.contains("UT")) {
                    return new BilingualText("康城", "LOHAS Park");
                } else {
                    return new BilingualText("北角/寶琳", "North Point/Po Lam");
                }
            }
            case "HAH":
            case "POA": {
                if (bound.contains("UT")) {
                    return new BilingualText("寶琳", "Po Lam");
                } else {
                    return new BilingualText("北角/康城", "North Point/LOHAS Park");
                }
            }
            case "AIR":
            case "AWE": {
                if (bound.contains("UT")) {
                    return new BilingualText("博覽館", "AsiaWorld-Expo");
                }
            }
        }
        return route.getDest();
    }

    public static boolean isMtrStopOnOrAfter(String stopId, String relativeTo, String lineName, String bound) {
        for (Route data : DATA_SHEET.getRouteList().values()) {
            if (lineName.equals(data.getRouteNumber()) && data.getBound().get("mtr").endsWith(bound)) {
                List<String> stopsList = data.getStops().get("mtr");
                int index = stopsList.indexOf(stopId);
                int indexRef = stopsList.indexOf(relativeTo);
                if (indexRef >= 0 && index >= indexRef) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMtrStopEndOfLine(String stopId, String lineName, String bound) {
        for (Route data : DATA_SHEET.getRouteList().values()) {
            if (lineName.equals(data.getRouteNumber()) && data.getBound().get("mtr").endsWith(bound)) {
                List<String> stopsList = data.getStops().get("mtr");
                int index = stopsList.indexOf(stopId);
                if (index >= 0 && index + 1 < stopsList.size()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static MTRInterchangeData getMtrStationInterchange(String stopId, String lineName) {
        Set<String> lines = new TreeSet<>(Comparator.comparing(Shared.Companion::getMtrLineSortingIndex));
        Set<String> outOfStationLines = new TreeSet<>(Comparator.comparing(Shared.Companion::getMtrLineSortingIndex));
        if (stopId.equals("KOW") || stopId.equals("AUS")) {
            outOfStationLines.add("HighSpeed");
        }
        boolean isOutOfStationPaid = !stopId.equals("ETS") && !stopId.equals("TST") && !stopId.equals("KOW") && !stopId.equals("AUS");
        boolean hasLightRail = false;
        String outOfStationStopName;
        switch (stopId) {
            case "ETS":
                outOfStationStopName = "尖沙咀";
                break;
            case "TST":
                outOfStationStopName = "尖東";
                break;
            case "HOK":
                outOfStationStopName = "中環";
                break;
            case "CEN":
                outOfStationStopName = "香港";
                break;
            default:
                outOfStationStopName = null;
                break;
        }
        String stopName = DATA_SHEET.getStopList().get(stopId).getName().getZh();
        for (Route data : DATA_SHEET.getRouteList().values()) {
            Map<String, String> bound = data.getBound();
            String routeNumber = data.getRouteNumber();
            if (!routeNumber.equals(lineName)) {
                if (bound.containsKey("mtr")) {
                    List<String> stopsList = data.getStops().get("mtr").stream().map(id -> DATA_SHEET.getStopList().get(id).getName().getZh()).collect(Collectors.toList());
                    if (stopsList.contains(stopName)) {
                        lines.add(routeNumber);
                    } else if (outOfStationStopName != null && stopsList.contains(outOfStationStopName)) {
                        outOfStationLines.add(routeNumber);
                    }
                } else if (bound.containsKey("lightRail") && !hasLightRail) {
                    if (data.getStops().get("lightRail").stream().anyMatch(id -> DATA_SHEET.getStopList().get((String) id).getName().getZh().equals(stopName))) {
                        hasLightRail = true;
                    }
                }
            }
        }
        return new MTRInterchangeData(lines, isOutOfStationPaid, outOfStationLines, hasLightRail);
    }

    public static class MTRInterchangeData {

        private final Set<String> lines;
        private final boolean isOutOfStationPaid;
        private final Set<String> outOfStationLines;
        private final boolean hasLightRail;

        public MTRInterchangeData(Set<String> lines, boolean isOutOfStationPaid, Set<String> outOfStationLines, boolean hasLightRail) {
            this.lines = lines;
            this.isOutOfStationPaid = isOutOfStationPaid;
            this.outOfStationLines = outOfStationLines;
            this.hasLightRail = hasLightRail;
        }

        public Set<String> getLines() {
            return lines;
        }

        public boolean isOutOfStationPaid() {
            return isOutOfStationPaid;
        }

        public Set<String> getOutOfStationLines() {
            return outOfStationLines;
        }

        public boolean isHasLightRail() {
            return hasLightRail;
        }
    }

    public static String getNoScheduledDepartureMessage(String altMessage, boolean isAboveTyphoonSignalEight, String typhoonWarningTitle) {
        if (altMessage == null || altMessage.isEmpty()) {
            altMessage = Shared.Companion.getLanguage().equals("en") ? "No scheduled departures at this moment" : "暫時沒有預定班次";
        }
        if (isAboveTyphoonSignalEight) {
            altMessage += " (" + typhoonWarningTitle + ")";
        }
        if (isAboveTyphoonSignalEight) {
            return "<span style=\"color: #88A3D1;\">" + altMessage + "</span>";
        } else {
            return altMessage;
        }
    }

    public static Future<TyphoonInfo> getCurrentTyphoonData() {
        long now = System.currentTimeMillis();
        if (typhoonInfo != null && now - typhoonInfoLastUpdated < 300000) {
            return CompletableFuture.completedFuture(typhoonInfo);
        }
        CompletableFuture<TyphoonInfo> future = new CompletableFuture<>();
        new Thread(() -> {
            JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=" + (Shared.Companion.getLanguage().equals("en") ? "en" : "tc"));
            if (data != null && data.has("WTCSGNL")) {
                Matcher matcher = Pattern.compile("TC([0-9]+)(.*)").matcher(data.optJSONObject("WTCSGNL").optString("code"));
                if (matcher.find() && matcher.group(1) != null) {
                    int signal = Integer.parseInt(matcher.group(1));
                    boolean isAboveTyphoonSignalEight = signal >= 8;
                    boolean isAboveTyphoonSignalNine = signal >= 9;
                    String typhoonWarningTitle;
                    if (Shared.Companion.getLanguage().equals("en")) {
                        typhoonWarningTitle = data.optJSONObject("WTCSGNL").optString("type") + " is in force";
                    } else {
                        typhoonWarningTitle = data.optJSONObject("WTCSGNL").optString("type") + " 現正生效";
                    }
                    String currentTyphoonSignalId;
                    if (signal < 8) {
                        currentTyphoonSignalId = "tc" + signal + (matcher.group(2) != null ? matcher.group(2) : "").toLowerCase();
                    } else {
                        currentTyphoonSignalId = "tc" + StringUtils.padStart(String.valueOf(signal), 2, '0') + (matcher.group(2) != null ? matcher.group(2) : "").toLowerCase();
                    }
                    typhoonInfo = new TyphoonInfo(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId);
                    typhoonInfoLastUpdated = System.currentTimeMillis();
                    future.complete(typhoonInfo);
                    return;
                }
            }
            typhoonInfo = TyphoonInfo.NO_TYPHOON;
            typhoonInfoLastUpdated = System.currentTimeMillis();
            future.complete(typhoonInfo);
        }).start();
        return future;
    }

    public static class TyphoonInfo {

        public static final TyphoonInfo NO_TYPHOON = new TyphoonInfo(false, false, "", "");

        private final boolean isAboveTyphoonSignalEight;
        private final boolean isAboveTyphoonSignalNine;
        private final String typhoonWarningTitle;
        private final String currentTyphoonSignalId;

        public TyphoonInfo(boolean isAboveTyphoonSignalEight, boolean isAboveTyphoonSignalNine, String typhoonWarningTitle, String currentTyphoonSignalId) {
            this.isAboveTyphoonSignalEight = isAboveTyphoonSignalEight;
            this.isAboveTyphoonSignalNine = isAboveTyphoonSignalNine;
            this.typhoonWarningTitle = typhoonWarningTitle;
            this.currentTyphoonSignalId = currentTyphoonSignalId;
        }

        public boolean isAboveTyphoonSignalEight() {
            return isAboveTyphoonSignalEight;
        }

        public boolean isAboveTyphoonSignalNine() {
            return isAboveTyphoonSignalNine;
        }

        public String getTyphoonWarningTitle() {
            return typhoonWarningTitle;
        }

        public String getCurrentTyphoonSignalId() {
            return currentTyphoonSignalId;
        }
    }

    public static ETAQueryResult getEta(String stopId, String co, Route route, Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return ETAQueryResult.connectionError(co);
        }
        CompletableFuture<ETAQueryResult> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                TyphoonInfo typhoonInfo = getCurrentTyphoonData().get();

                Map<Integer, String> lines = new HashMap<>();
                long nextScheduledBus = -999;
                boolean isMtrEndOfLine = false;
                boolean isTyphoonSchedule = false;
                String nextCo = co;

                lines.put(1, getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle()));
                String language = Shared.Companion.getLanguage();
                if (route.isKmbCtbJoint()) {
                    isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();
                    String dest = route.getDest().getZh().replace(" ", "");
                    String orig = route.getOrig().getZh().replace(" ", "");
                    Map<Long, Pair<String, String>> etaSorted = new TreeMap<>();
                    String kmbSpecialMessage = null;
                    long kmbFirstScheduledBus = Long.MAX_VALUE;
                    {
                        JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/" + stopId);
                        JSONArray buses = data.optJSONArray("data");

                        for (int u = 0; u < buses.length(); u++) {
                            JSONObject bus = buses.optJSONObject(u);
                            if ("KMB".equals(bus.optString("co"))) {
                                String routeNumber = bus.optString("route");
                                String bound = bus.optString("dir");
                                if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get("kmb"))) {
                                    String eta = bus.optString("eta");
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                    if (!eta.isEmpty() && !eta.equalsIgnoreCase("null")) {
                                        long mins = Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> Min.</small>";
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> Min.</small>";
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> 分鐘</small>";
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> 分鐘</small>";
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : "<small> (" + bus.optString("rmk_tc") + ")</small>");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");
                                        if ((message.contains("預定班次") || message.contains("Scheduled Bus")) && mins < kmbFirstScheduledBus) {
                                            kmbFirstScheduledBus = mins;
                                        }
                                        etaSorted.put(mins, Pair.create(message, "kmb"));
                                    } else {
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                            }
                                        } else {
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : "<small> (" + bus.optString("rmk_tc") + ")</small>");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");
                                        if (message.isEmpty() || (typhoonInfo.isAboveTyphoonSignalEight() && (message.equals("ETA service suspended") || message.equals("暫停預報")))) {
                                            message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                        } else {
                                            message = "<b></b>" + message;
                                        }
                                        kmbSpecialMessage = message;
                                    }
                                }
                            }
                        }
                    }
                    {
                        String routeNumber = route.getRouteNumber();
                        List<List<String>> matchingStops = DATA_SHEET.getStopMap().get(stopId);
                        List<String> ctbStopIds = new ArrayList<>();
                        if (matchingStops != null) {
                            for (List<String> stopArray : matchingStops) {
                                if ("ctb".equals(stopArray.get(0))) {
                                    ctbStopIds.add(stopArray.get(1));
                                }
                            }
                        }
                        Map<String, Map<Long, Pair<String, String>>> ctbEtaEntries = new HashMap<>();
                        ctbEtaEntries.put(dest, new HashMap<>());
                        ctbEtaEntries.put(orig, new HashMap<>());
                        for (String ctbStopId : ctbStopIds) {
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/" + ctbStopId + "/" + routeNumber);
                            JSONArray buses = data.optJSONArray("data");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if ("CTB".equals(bus.optString("co"))) {
                                    String busDest = bus.optString("dest_tc").replace(" ", "");
                                    if (routeNumber.equals(bus.optString("route"))) {
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        if (!eta.isEmpty() && !eta.equalsIgnoreCase("null")) {
                                            long mins = Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                            String message = "";
                                            if (language.equals("en")) {
                                                if (mins > 0) {
                                                    message = "<b>" + mins + "</b><small> Min.</small>";
                                                } else if (mins > -60) {
                                                    message = "<b>-</b><small> Min.</small>";
                                                }
                                                if (!bus.optString("rmk_en").isEmpty()) {
                                                    message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                                }
                                            } else {
                                                if (mins > 0) {
                                                    message = "<b>" + mins + "</b><small> 分鐘</small>";
                                                } else if (mins > -60) {
                                                    message = "<b>-</b><small> 分鐘</small>";
                                                }
                                                if (!bus.optString("rmk_tc").isEmpty()) {
                                                    message += (message.isEmpty() ? bus.optString("rmk_tc") : "<small> (" + bus.optString("rmk_tc") + ")</small>");
                                                }
                                            }
                                            message = message
                                                    .replaceAll("原定", "預定")
                                                    .replaceAll("最後班次", "尾班車")
                                                    .replaceAll("尾班車已過", "尾班車已過本站");
                                            ctbEtaEntries.entrySet().stream().min(Comparator.comparing(e -> StringUtils.editDistance(e.getKey(), busDest))).orElseThrow(RuntimeException::new)
                                                    .getValue().put(mins, Pair.create(message, "ctb"));
                                        }
                                    }
                                }
                            }
                        }
                        etaSorted.putAll(ctbEtaEntries.get(dest));
                    }

                    if (etaSorted.isEmpty()) {
                        if (kmbSpecialMessage == null || kmbSpecialMessage.isEmpty()) {
                            lines.put(1, getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle()));
                        } else {
                            lines.put(1, kmbSpecialMessage);
                        }
                    } else {
                        int counter = 0;
                        for (Map.Entry<Long, Pair<String, String>> entry : etaSorted.entrySet()) {
                            long mins = entry.getKey();
                            String message = "<b></b>" + entry.getValue().first.replace("(尾班車)", "").replace("(Final Bus)", "").trim();
                            String entryCo = entry.getValue().second;
                            if (mins > kmbFirstScheduledBus && !(message.contains("預定班次") || message.contains("Scheduled Bus"))) {
                                message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " (Scheduled Bus)" : " (預定班次)") + "</small>";
                            }
                            if (entryCo.equals("kmb")) {
                                if (Shared.Companion.getKMBSubsidiary(route.getRouteNumber()) == KMBSubsidiary.LWB) {
                                    message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " - LWB" : " - 龍運") + "</small>";
                                } else {
                                    message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " - KMB" : " - 九巴") + "</small>";
                                }
                            } else {
                                message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " - CTB" : " - 城巴") + "</small>";
                            }
                            int seq = ++counter;
                            if (seq == 1) {
                                nextScheduledBus = mins;
                                nextCo = entryCo;
                            }
                            lines.put(seq, message);
                        }
                    }
                } else {
                    switch (co) {
                        case "kmb": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/" + stopId);
                            JSONArray buses = data.optJSONArray("data");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if ("KMB".equals(bus.optString("co"))) {
                                    String routeNumber = bus.optString("route");
                                    String bound = bus.optString("dir");
                                    if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get("kmb"))) {
                                        int seq = bus.optInt("eta_seq");
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : "<small> (" + bus.optString("rmk_tc") + ")</small>");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty() || (typhoonInfo.isAboveTyphoonSignalEight() && (message.equals("ETA service suspended") || message.equals("暫停預報")))) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                            } else {
                                                message = "<b></b>-";
                                            }
                                        } else {
                                            message = "<b></b>" + message;
                                        }
                                        lines.put(seq, message);
                                    }
                                }
                            }
                            break;
                        }
                        case "ctb": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                            String routeNumber = route.getRouteNumber();
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/" + stopId + "/" + routeNumber);
                            JSONArray buses = data.optJSONArray("data");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if ("CTB".equals(bus.optString("co"))) {
                                    String bound = bus.optString("dir");
                                    if (routeNumber.equals(bus.optString("route")) && bound.equals(route.getBound().get("ctb"))) {
                                        int seq = bus.optInt("eta_seq");
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : "<small> (" + bus.optString("rmk_tc") + ")</small>");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty()) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                            } else {
                                                message = "<b></b>-";
                                            }
                                        } else {
                                            message = "<b></b>" + message;
                                        }
                                        lines.put(seq, message);
                                    }
                                }
                            }
                            break;
                        }
                        case "nlb": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=" + route.getNlbId() + "&stopId=" + stopId + "&language=" + Shared.Companion.getLanguage());
                            if (data == null || data.length() == 0 || !data.has("estimatedArrivals")) {
                                return;
                            }
                            JSONArray buses = data.optJSONArray("estimatedArrivals");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                int seq = u + 1;
                                String eta = bus.optString("estimatedArrivalTime") + "+08:00";
                                String variant = bus.optString("routeVariantName").trim();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
                                long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                String message = "";
                                if (language.equals("en")) {
                                    if (mins > 0) {
                                        message = "<b>" + mins + "</b><small> Min.</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b><small> Min.</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                } else {
                                    if (mins > 0) {
                                        message = "<b>" + mins + "</b><small> 分鐘</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b><small> 分鐘</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                }
                                if (!variant.isEmpty()) {
                                    message += (message.isEmpty() ? variant : "<small> (" + variant + ")</small>");
                                }
                                message = message
                                        .replaceAll("原定", "預定")
                                        .replaceAll("最後班次", "尾班車")
                                        .replaceAll("尾班車已過", "尾班車已過本站");

                                if (message.isEmpty()) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                    } else {
                                        message = "<b></b>-";
                                    }
                                } else {
                                    message = "<b></b>" + message;
                                }
                                lines.put(seq, message);
                            }
                            break;
                        }
                        case "mtr-bus": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                            String routeNumber = route.getRouteNumber();
                            JSONObject body = new JSONObject();
                            try {
                                body.put("language", Shared.Companion.getLanguage());
                                body.put("routeName", routeNumber);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            JSONObject data = HTTPRequestUtils.postJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/bus/getSchedule", body);
                            JSONArray busStops = data.optJSONArray("busStop");
                            for (int k = 0; k < busStops.length(); k++) {
                                JSONObject busStop = busStops.optJSONObject(k);
                                JSONArray buses = busStop.optJSONArray("bus");
                                String busStopId = busStop.optString("busStopId");
                                for (int u = 0; u < buses.length(); u++) {
                                    JSONObject bus = buses.optJSONObject(u);
                                    int seq = u + 1;
                                    double eta = bus.optDouble("arrivalTimeInSecond");
                                    if (eta >= 108000) {
                                        eta = bus.optDouble("departureTimeInSecond");
                                    }
                                    String remark = bus.optString("busRemark");
                                    if (remark == null || remark.isEmpty() || remark.equalsIgnoreCase("null")) {
                                        remark = "";
                                    }
                                    boolean isScheduled = bus.optString("isScheduled").equals("1");
                                    if (isScheduled) {
                                        if (!remark.isEmpty()) {
                                            remark += "/";
                                        }
                                        remark += language.equals("en") ? "Scheduled Bus" : "預定班次";
                                    }
                                    boolean isDelayed = bus.optString("isDelayed").equals("1");
                                    if (isDelayed) {
                                        if (!remark.isEmpty()) {
                                            remark += "/";
                                        }
                                        remark += language.equals("en") ? "Bus Delayed" : "行車緩慢";
                                    }

                                    long mins = (long) Math.floor(eta / 60);

                                    if (MTR_BUS_STOP_ALIAS.get(stopId).contains(busStopId)) {
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> Min.</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b><small> 分鐘</small>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                        }
                                        if (!remark.isEmpty()) {
                                            message += (message.isEmpty() ? remark : "<small> (" + remark + ")</small>");
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty()) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                            } else {
                                                message = "<b></b>-";
                                            }
                                        } else {
                                            message = "<b></b>" + message;
                                        }
                                        lines.put(seq, message);
                                    }
                                }
                            }
                            break;
                        }
                        case "gmb": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etagmb.gov.hk/eta/stop/" + stopId);
                            List<Pair<Long, JSONObject>> busList = new ArrayList<>();
                            for (int i = 0; i < data.optJSONArray("data").length(); i++) {
                                JSONObject routeData = data.optJSONArray("data").optJSONObject(i);
                                JSONArray buses = routeData.optJSONArray("eta");
                                Optional<Route> filteredEntry = DATA_SHEET.getRouteList().values().stream()
                                        .filter(r -> r.getBound().containsKey("gmb") && r.getGtfsId().equals(routeData.optString("route_id")))
                                        .findFirst();
                                if (filteredEntry.isPresent() && buses != null) {
                                    String routeNumber = filteredEntry.get().getRouteNumber();
                                    for (int u = 0; u < buses.length(); u++) {
                                        JSONObject bus = buses.optJSONObject(u);
                                        String eta = bus.optString("timestamp");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        if (routeNumber.equals(route.getRouteNumber())) {
                                            busList.add(Pair.create(mins, bus));
                                        }
                                    }
                                }
                            }
                            busList.sort(Comparator.comparing(p -> p.first));
                            for (int i = 0; i < busList.size(); i++) {
                                Pair<Long, JSONObject> entry = busList.get(i);
                                JSONObject bus = entry.second;
                                int seq = i + 1;
                                String remark = language.equals("en") ? bus.optString("remarks_en") : bus.optString("remarks_tc");
                                if (remark == null || remark.equalsIgnoreCase("null")) {
                                    remark = "";
                                }
                                long mins = entry.first;
                                String message = "";
                                if (language.equals("en")) {
                                    if (mins > 0) {
                                        message = "<b>" + mins + "</b><small> Min.</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b><small> Min.</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                } else {
                                    if (mins > 0) {
                                        message = "<b>" + mins + "</b><small> 分鐘</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b><small> 分鐘</small>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                }
                                if (!remark.isEmpty()) {
                                    message += (message.isEmpty() ? remark : "<small> (" + remark + ")</small>");
                                }
                                message = message
                                        .replaceAll("原定", "預定")
                                        .replaceAll("最後班次", "尾班車")
                                        .replaceAll("尾班車已過", "尾班車已過本站");

                                if (message.isEmpty()) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                    } else {
                                        message = "<b></b>-";
                                    }
                                } else {
                                    message = "<b></b>" + message;
                                }
                                lines.put(seq, message);
                            }
                            break;
                        }
                        case "lightRail": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine();

                            List<String> stopsList = route.getStops().get("lightRail");
                            if (stopsList.indexOf(stopId) + 1 >= stopsList.size()) {
                                isMtrEndOfLine = true;
                                lines.put(1, Shared.Companion.getLanguage().equals("en") ? "End of Line" : "終點站");
                            } else {
                                List<LrtETAData> results = new ArrayList<>();
                                JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=" + stopId.substring(2));
                                if (data.optInt("status") == 0) {
                                    lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊");
                                } else {
                                    JSONArray platformList = data.optJSONArray("platform_list");
                                    for (int i = 0; i < platformList.length(); i++) {
                                        JSONObject platform = platformList.optJSONObject(i);
                                        int platformNumber = platform.optInt("platform_id");
                                        JSONArray routeList = platform.optJSONArray("route_list");
                                        if (routeList != null) {
                                            for (int u = 0; u < routeList.length(); u++) {
                                                JSONObject routeData = routeList.optJSONObject(u);
                                                String routeNumber = routeData.optString("route_no");
                                                if (routeNumber.equals(route.getRouteNumber())) {
                                                    Matcher matcher = Pattern.compile("([0-9]+) *min").matcher(routeData.optString("time_en"));
                                                    long mins = matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
                                                    String minsMsg = routeData.optString(Shared.Companion.getLanguage().equals("en") ? "time_en" : "time_ch");
                                                    String dest = routeData.optString(Shared.Companion.getLanguage().equals("en") ? "dest_en" : "dest_ch");
                                                    int trainLength = routeData.optInt("train_length");
                                                    results.add(new LrtETAData(routeNumber, dest, trainLength, platformNumber, mins, minsMsg));
                                                }
                                            }
                                        }
                                    }
                                    results.sort(Comparator.naturalOrder());
                                    for (int i = 0; i < results.size(); i++) {
                                        LrtETAData lrt = results.get(i);
                                        int seq = i + 1;
                                        String minsMessage = lrt.getEtaMessage();
                                        if (minsMessage.equals("-")) {
                                            minsMessage = Shared.Companion.getLanguage().equals("en") ? "Departing" : "正在離開";
                                        }
                                        if (minsMessage.equals("即將抵達") || minsMessage.equals("Arriving") || minsMessage.equals("正在離開") || minsMessage.equals("Departing")) {
                                            minsMessage = "<b>" + minsMessage + "</b>";
                                        } else {
                                            minsMessage = minsMessage.replaceAll("^([0-9]+)", "<b>$1</b>").replace(" min", "<small> Min.</small>").replace(" 分鐘", "<small> 分鐘</small>");
                                        }
                                        if (seq == 1) {
                                            nextScheduledBus = lrt.getEta();
                                        }
                                        StringBuilder cartsMessage = new StringBuilder(Math.max(3, lrt.getTrainLength() * 2));
                                        int lrv = R.mipmap.lrv;
                                        int lrvEmpty = R.mipmap.lrv_empty;
                                        for (int u = 0; u < lrt.getTrainLength(); u++) {
                                            cartsMessage.append("<img src=\"lrv\">");
                                        }
                                        if (lrt.getTrainLength() == 1) {
                                            cartsMessage.append("<img src=\"lrv_empty\">");
                                        }
                                        String message = "<b></b><span style=\"color: #D3A809\">" + StringUtils.getCircledNumber(lrt.getPlatformNumber()) + "</span> " + cartsMessage + " " + minsMessage;
                                        lines.put(seq, message);
                                    }
                                }
                            }
                            break;
                        }
                        case "mtr": {
                            isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine();

                            String lineName = route.getRouteNumber();
                            String lineColor;
                            switch (lineName) {
                                case "AEL":
                                    lineColor = "#00888E";
                                    break;
                                case "TCL":
                                    lineColor = "#F3982D";
                                    break;
                                case "TML":
                                    lineColor = "#9C2E00";
                                    break;
                                case "TKL":
                                    lineColor = "#7E3C93";
                                    break;
                                case "EAL":
                                    lineColor = "#5EB7E8";
                                    break;
                                case "SIL":
                                    lineColor = "#CBD300";
                                    break;
                                case "TWL":
                                    lineColor = "#E60012";
                                    break;
                                case "ISL":
                                    lineColor = "#0075C2";
                                    break;
                                case "KTL":
                                    lineColor = "#00A040";
                                    break;
                                case "DRL":
                                    lineColor = "#EB6EA5";
                                    break;
                                default:
                                    lineColor = "#FFFFFF";
                            }

                            String bound = route.getBound().get("mtr");
                            if (isMtrStopEndOfLine(stopId, lineName, bound)) {
                                isMtrEndOfLine = true;
                                lines.put(1, Shared.Companion.getLanguage().equals("en") ? "End of Line" : "終點站");
                            } else {
                                ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
                                int hour = hongKongTime.getHour();
                                boolean isOutOfServiceHours = hour >= 0 && hour < 5;

                                JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + lineName + "&sta=" + stopId);
                                if (data.optInt("status") == 0) {
                                    lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊");
                                } else {
                                    JSONObject lineStops = data.optJSONObject("data").optJSONObject(lineName + "-" + stopId);
                                    if (lineStops == null) {
                                        if (stopId.equals("RAC")) {
                                            lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Service on race days only" : "僅在賽馬日提供服務");
                                        } else if (isOutOfServiceHours) {
                                            lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出");
                                        } else {
                                            lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊");
                                        }
                                    } else {
                                        boolean delayed = !data.optString("isdelay", "N").equals("N");
                                        String dir = bound.equals("UT") ? "UP" : "DOWN";
                                        JSONArray trains = lineStops.optJSONArray(dir);
                                        if (trains == null || trains.length() == 0) {
                                            if (stopId.equals("RAC")) {
                                                lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Service on race days only" : "僅在賽馬日提供服務");
                                            } else if (isOutOfServiceHours) {
                                                lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出");
                                            } else {
                                                lines.put(1, Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊");
                                            }
                                        } else {
                                            for (int u = 0; u < trains.length(); u++) {
                                                JSONObject trainData = trains.optJSONObject(u);
                                                int seq = Integer.parseInt(trainData.optString("seq"));
                                                int platform = Integer.parseInt(trainData.optString("plat"));
                                                String specialRoute = trainData.optString("route");
                                                String dest = DATA_SHEET.getStopList().get(trainData.optString("dest")).getName().get(Shared.Companion.getLanguage());
                                                if (!stopId.equals("AIR")) {
                                                    if (dest.equals("博覽館")) {
                                                        dest = "機場及博覽館";
                                                    } else if (dest.equals("AsiaWorld-Expo")) {
                                                        dest = "Airport & AsiaWorld-Expo";
                                                    }
                                                }
                                                if (!specialRoute.isEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                                    String via = DATA_SHEET.getStopList().get(specialRoute).getName().get(Shared.Companion.getLanguage());
                                                    dest += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " via " : " 經") + via + "</small>";
                                                }
                                                String eta = trainData.optString("time");

                                                @SuppressLint("SimpleDateFormat")
                                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                format.setTimeZone(TimeZone.getTimeZone(hongKongTime.getZone()));
                                                long mins = (long) Math.ceil((format.parse(eta).getTime() - Instant.now().toEpochMilli()) / 60000.0);

                                                String minsMessage;
                                                if (mins > 1) {
                                                    minsMessage = "<b>" + mins + "</b><small>" + (Shared.Companion.getLanguage().equals("en") ? " Min." : " 分鐘") + "</small>";
                                                } else if (mins == 1) {
                                                    minsMessage = "<b>" + (Shared.Companion.getLanguage().equals("en") ? "Arriving" : "即將抵達") + "</b>";
                                                } else {
                                                    minsMessage = "<b>" + (Shared.Companion.getLanguage().equals("en") ? "Departing" : "正在離開") + "</b>";
                                                }

                                                String message = "<b></b><span style=\"color: " + lineColor + "\">" + StringUtils.getCircledNumber(platform) + "</span> " + dest + " " + minsMessage;
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                    if (delayed) {
                                                        message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " (Delayed)" : " (服務延誤)") + "</small>";
                                                    }
                                                }
                                                lines.put(seq, message);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                future.complete(ETAQueryResult.result(nextScheduledBus > -60 ? Math.max(0, nextScheduledBus) : -1, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines));
            } catch (Throwable e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        }).start();
        try {
            return future.get(9, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException | CancellationException e) {
            try {
                future.cancel(true);
            } catch (Throwable ignore) {}
            return ETAQueryResult.connectionError(co);
        }
    }

    public static class LrtETAData implements Comparable<LrtETAData> {

        public static final Comparator<LrtETAData> COMPARATOR = Comparator.comparing(LrtETAData::getEta).thenComparing(LrtETAData::getPlatformNumber);

        private final String routeNumber;
        private final String dest;
        private final int trainLength;
        private final int platformNumber;
        private final long eta;
        private final String etaMessage;

        public LrtETAData(String routeNumber, String dest, int trainLength, int platformNumber, long eta, String etaMessage) {
            this.routeNumber = routeNumber;
            this.dest = dest;
            this.trainLength = trainLength;
            this.platformNumber = platformNumber;
            this.eta = eta;
            this.etaMessage = etaMessage;
        }

        public String getRouteNumber() {
            return routeNumber;
        }

        public String getDest() {
            return dest;
        }

        public int getTrainLength() {
            return trainLength;
        }

        public int getPlatformNumber() {
            return platformNumber;
        }

        public long getEta() {
            return eta;
        }

        public String getEtaMessage() {
            return etaMessage;
        }

        @Override
        public int compareTo(LrtETAData o) {
            return COMPARATOR.compare(this, o);
        }
    }

    @Stable
    public static class ETAQueryResult {

        public static final ETAQueryResult EMPTY = new ETAQueryResult(true, -1, false, false, null, Collections.emptyMap());

        public static ETAQueryResult connectionError(String co) {
            return new ETAQueryResult(true, -1, false, false, co, Collections.singletonMap(1, Shared.Companion.getLanguage().equals("en") ? "Unable to Connect" : "無法連接伺服器"));
        }

        public static ETAQueryResult result(long nextScheduledBus, boolean isMtrEndOfLine, boolean isTyphoonSchedule, String nextCo, Map<Integer, String> lines) {
            return new ETAQueryResult(false, nextScheduledBus, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines);
        }

        private final boolean isConnectionError;
        private final long nextScheduledBus;
        private final boolean isMtrEndOfLine;
        private final boolean isTyphoonSchedule;
        private final String nextCo;
        private final Map<Integer, String> lines;

        private ETAQueryResult(boolean isConnectionError, long nextScheduledBus, boolean isMtrEndOfLine, boolean isTyphoonSchedule, String nextCo, Map<Integer, String> lines) {
            this.isConnectionError = isConnectionError;
            this.nextScheduledBus = nextScheduledBus;
            this.isMtrEndOfLine = isMtrEndOfLine;
            this.isTyphoonSchedule = isTyphoonSchedule;
            this.nextCo = nextCo;
            this.lines = Collections.unmodifiableMap(lines);
        }

        public boolean isConnectionError() {
            return isConnectionError;
        }

        public long getNextScheduledBus() {
            return nextScheduledBus;
        }

        public boolean isMtrEndOfLine() {
            return isMtrEndOfLine;
        }

        public boolean isTyphoonSchedule() {
            return isTyphoonSchedule;
        }

        public String getNextCo() {
            return nextCo;
        }

        public String getLine(int index) {
            return lines.getOrDefault(index, "-");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ETAQueryResult result = (ETAQueryResult) o;
            return isConnectionError == result.isConnectionError && nextScheduledBus == result.nextScheduledBus && isMtrEndOfLine == result.isMtrEndOfLine && isTyphoonSchedule == result.isTyphoonSchedule && Objects.equals(nextCo, result.nextCo) && Objects.equals(lines, result.lines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isConnectionError, nextScheduledBus, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines);
        }
    }

    public enum State {

        LOADING, UPDATING, READY, ERROR;

    }

}
