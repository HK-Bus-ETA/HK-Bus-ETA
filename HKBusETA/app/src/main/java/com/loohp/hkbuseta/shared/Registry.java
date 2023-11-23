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

import static com.loohp.hkbuseta.objects.RouteExtensionsKt.prependTo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.compose.runtime.Immutable;
import androidx.core.app.ComponentActivity;
import androidx.core.util.AtomicFile;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileUpdateRequester;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.loohp.hkbuseta.branchedlist.BranchedList;
import com.loohp.hkbuseta.objects.BilingualText;
import com.loohp.hkbuseta.objects.Coordinates;
import com.loohp.hkbuseta.objects.DataContainer;
import com.loohp.hkbuseta.objects.FavouriteRouteStop;
import com.loohp.hkbuseta.objects.GMBRegion;
import com.loohp.hkbuseta.objects.Operator;
import com.loohp.hkbuseta.objects.Preferences;
import com.loohp.hkbuseta.objects.Route;
import com.loohp.hkbuseta.objects.RouteExtensionsKt;
import com.loohp.hkbuseta.objects.RouteListType;
import com.loohp.hkbuseta.objects.RouteSearchResultEntry;
import com.loohp.hkbuseta.objects.RouteSortMode;
import com.loohp.hkbuseta.objects.Stop;
import com.loohp.hkbuseta.tiles.EtaTileService;
import com.loohp.hkbuseta.tiles.EtaTileServiceEight;
import com.loohp.hkbuseta.tiles.EtaTileServiceFive;
import com.loohp.hkbuseta.tiles.EtaTileServiceFour;
import com.loohp.hkbuseta.tiles.EtaTileServiceOne;
import com.loohp.hkbuseta.tiles.EtaTileServiceSeven;
import com.loohp.hkbuseta.tiles.EtaTileServiceSix;
import com.loohp.hkbuseta.tiles.EtaTileServiceThree;
import com.loohp.hkbuseta.tiles.EtaTileServiceTwo;
import com.loohp.hkbuseta.utils.CollectionsUtils;
import com.loohp.hkbuseta.utils.CollectionsUtilsKtKt;
import com.loohp.hkbuseta.utils.ConnectionUtils;
import com.loohp.hkbuseta.utils.DistanceUtils;
import com.loohp.hkbuseta.utils.HTTPRequestUtils;
import com.loohp.hkbuseta.utils.IntUtils;
import com.loohp.hkbuseta.utils.StateUtilsKtKt;
import com.loohp.hkbuseta.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import kotlin.Pair;
import kotlin.Triple;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;

public class Registry {

    private static volatile Registry INSTANCE = null;
    private static final Object INSTANCE_LOCK = new Object();
    private static final ExecutorService ETA_QUERY_EXECUTOR = new ThreadPoolExecutor(64, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>());

    public static Registry getInstance(Context context) {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new Registry(context, false);
            }
            return INSTANCE;
        }
    }

    public static Registry getInstanceNoUpdateCheck(Context context) {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new Registry(context, true);
            }
            return INSTANCE;
        }
    }

    public static boolean hasInstanceCreated() {
        synchronized (INSTANCE_LOCK) {
            return INSTANCE != null;
        }
    }

    public static void clearInstance() {
        synchronized (INSTANCE_LOCK) {
            INSTANCE = null;
        }
    }

    public static Registry initInstanceWithImportedPreference(Context context, JSONObject preferencesData) {
        synchronized (INSTANCE_LOCK) {
            try {
                INSTANCE = new Registry(context, true, preferencesData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return INSTANCE;
        }
    }

    private static final String PREFERENCES_FILE_NAME = "preferences.json";
    private static final String CHECKSUM_FILE_NAME = "checksum.json";
    private static final String DATA_FILE_NAME = "data.json";

    public static void invalidateCache(Context context) {
        try { context.getApplicationContext().deleteFile(CHECKSUM_FILE_NAME); } catch (Throwable ignore) {}
    }

    private Preferences PREFERENCES = null;
    private DataContainer DATA = null;

    private final MutableStateFlow<TyphoonInfo> typhoonInfo = StateUtilsKtKt.asMutableStateFlow(TyphoonInfo.NULL);

    private final MutableStateFlow<State> state = StateUtilsKtKt.asMutableStateFlow(State.LOADING);
    private final MutableStateFlow<Float> updatePercentageState = StateUtilsKtKt.asMutableStateFlow(0F);
    private final Object preferenceWriteLock = new Object();
    private final AtomicLong lastUpdateCheck = new AtomicLong(0);
    private final AtomicReference<Future<?>> currentChecksumTask = new AtomicReference<>(null);

    private Registry(Context context, boolean suppressUpdateCheck) {
        try {
            ensureData(context, suppressUpdateCheck);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Registry(Context context, boolean suppressUpdateCheck, JSONObject importPreferencesData) throws JSONException {
        importPreference(context, importPreferencesData);
        try {
            ensureData(context, suppressUpdateCheck);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void savePreferences(Context context) throws IOException, JSONException {
        synchronized (preferenceWriteLock) {
            AtomicFile atomicFile = new AtomicFile(context.getApplicationContext().getFileStreamPath(PREFERENCES_FILE_NAME));
            try (FileOutputStream fos = atomicFile.startWrite();
                 PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.serialize().toString());
                pw.flush();
                atomicFile.finishWrite(fos);
            }
        }
    }

    private void importPreference(Context context, JSONObject preferencesData) throws JSONException {
        Preferences preferences = Preferences.deserialize(preferencesData).cleanForImport();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
            pw.write(preferences.serialize().toString());
            pw.flush();
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject exportPreference() throws JSONException {
        synchronized (preferenceWriteLock) {
            return PREFERENCES.serialize();
        }
    }

    public StateFlow<State> getState() {
        return state;
    }

    public StateFlow<Float> getUpdatePercentageState() {
        return updatePercentageState;
    }

    public void updateTileService(Context context) {
        TileUpdateRequester updater = TileService.getUpdater(context);
        updater.requestUpdate(EtaTileService.class);
        updater.requestUpdate(EtaTileServiceOne.class);
        updater.requestUpdate(EtaTileServiceTwo.class);
        updater.requestUpdate(EtaTileServiceThree.class);
        updater.requestUpdate(EtaTileServiceFour.class);
        updater.requestUpdate(EtaTileServiceFive.class);
        updater.requestUpdate(EtaTileServiceSix.class);
        updater.requestUpdate(EtaTileServiceSeven.class);
        updater.requestUpdate(EtaTileServiceEight.class);
    }

    public void updateTileService(int favoriteIndex, Context context) {
        switch (favoriteIndex) {
            case 1: { TileService.getUpdater(context).requestUpdate(EtaTileServiceOne.class); break; }
            case 2: { TileService.getUpdater(context).requestUpdate(EtaTileServiceTwo.class); break; }
            case 3: { TileService.getUpdater(context).requestUpdate(EtaTileServiceThree.class); break; }
            case 4: { TileService.getUpdater(context).requestUpdate(EtaTileServiceFour.class); break; }
            case 5: { TileService.getUpdater(context).requestUpdate(EtaTileServiceFive.class); break; }
            case 6: { TileService.getUpdater(context).requestUpdate(EtaTileServiceSix.class); break; }
            case 7: { TileService.getUpdater(context).requestUpdate(EtaTileServiceSeven.class); break; }
            case 8: { TileService.getUpdater(context).requestUpdate(EtaTileServiceEight.class); break; }
            default: { TileService.getUpdater(context).requestUpdate(EtaTileService.class); break; }
        }
    }

    public void setLanguage(String language, Context context) {
        Shared.Companion.setLanguage(language);
        try {
            PREFERENCES.setLanguage(language);
            savePreferences(context);
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasFavouriteRouteStop(int favoriteIndex) {
        return Shared.Companion.getFavoriteRouteStops().get(favoriteIndex) != null;
    }

    public boolean isFavouriteRouteStop(int favoriteIndex, String stopId, Operator co, int index, Stop stop, Route route) {
        FavouriteRouteStop favouriteRouteStop = Shared.Companion.getFavoriteRouteStops().get(favoriteIndex);
        if (favouriteRouteStop == null) {
            return false;
        }
        if (!stopId.equals(favouriteRouteStop.getStopId())) {
            return false;
        }
        if (co != favouriteRouteStop.getCo()) {
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
        clearFavouriteRouteStop(favoriteIndex, true, context);
    }

    private void clearFavouriteRouteStop(int favoriteIndex, boolean save, Context context) {
        try {
            Shared.Companion.updateFavoriteRouteStops(m -> m.remove(favoriteIndex));
            PREFERENCES.getFavouriteRouteStops().remove(favoriteIndex);
            Map<Integer, List<Integer>> changes = new HashMap<>();
            List<Integer> deletions = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> entry : Shared.Companion.getRawEtaTileConfigurations().entrySet()) {
                if (entry.getValue().contains(favoriteIndex)) {
                    List<Integer> updated = new ArrayList<>(entry.getValue());
                    updated.remove((Integer) favoriteIndex);
                    if (updated.isEmpty()) {
                        deletions.add(entry.getKey());
                    } else {
                        changes.put(entry.getKey(), updated);
                    }
                }
            }
            PREFERENCES.getEtaTileConfigurations().putAll(changes);
            deletions.forEach(i -> PREFERENCES.getEtaTileConfigurations().remove(i));
            Shared.Companion.updateEtaTileConfigurations(m -> {
                m.putAll(changes);
                deletions.forEach(m::remove);
            });
            if (save) {
                savePreferences(context);
                updateTileService(0, context);
                updateTileService(favoriteIndex, context);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFavouriteRouteStop(int favoriteIndex, String stopId, Operator co, int index, Stop stop, Route route, Context context) {
        setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, false, true, context);
    }

    private void setFavouriteRouteStop(int favoriteIndex, String stopId, Operator co, int index, Stop stop, Route route, boolean bypassEtaTileCheck, boolean save, Context context) {
        try {
            FavouriteRouteStop favouriteRouteStop = new FavouriteRouteStop(stopId, co, index, stop, route);
            Shared.Companion.updateFavoriteRouteStops(m -> m.put(favoriteIndex, favouriteRouteStop));
            PREFERENCES.getFavouriteRouteStops().put(favoriteIndex, favouriteRouteStop);
            if (!bypassEtaTileCheck) {
                Map<Integer, List<Integer>> changes = new HashMap<>();
                List<Integer> deletions = new ArrayList<>();
                for (Map.Entry<Integer, List<Integer>> entry : Shared.Companion.getRawEtaTileConfigurations().entrySet()) {
                    if (entry.getValue().contains(favoriteIndex)) {
                        List<Integer> updated = new ArrayList<>(entry.getValue());
                        updated.remove((Integer) favoriteIndex);
                        if (updated.isEmpty()) {
                            deletions.add(entry.getKey());
                        } else {
                            changes.put(entry.getKey(), updated);
                        }
                    }
                }
                PREFERENCES.getEtaTileConfigurations().putAll(changes);
                deletions.forEach(i -> PREFERENCES.getEtaTileConfigurations().remove(i));
                Shared.Companion.updateEtaTileConfigurations(m -> {
                    m.putAll(changes);
                    deletions.forEach(m::remove);
                });
            }
            if (save) {
                savePreferences(context);
                updateTileService(0, context);
                updateTileService(favoriteIndex, context);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearEtaTileConfiguration(int tileId, Context context) {
        try {
            Shared.Companion.updateEtaTileConfigurations(m -> m.remove(tileId));
            PREFERENCES.getEtaTileConfigurations().remove(tileId);
            savePreferences(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEtaTileConfiguration(int tileId, List<Integer> favouriteIndexes, Context context) {
        try {
            Shared.Companion.updateEtaTileConfigurations(m -> m.put(tileId, favouriteIndexes));
            PREFERENCES.getEtaTileConfigurations().put(tileId, favouriteIndexes);
            savePreferences(context);
            updateTileService(0, context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLastLookupRoute(String routeNumber, Operator co, String meta, Context context) {
        try {
            Shared.Companion.addLookupRoute(routeNumber, co, meta);
            List<LastLookupRoute> lastLookupRoutes = Shared.Companion.getLookupRoutes();
            PREFERENCES.getLastLookupRoutes().clear();
            PREFERENCES.getLastLookupRoutes().addAll(lastLookupRoutes);
            savePreferences(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLastLookupRoutes(Context context) {
        try {
            Shared.Companion.clearLookupRoute();
            PREFERENCES.getLastLookupRoutes().clear();
            savePreferences(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRouteSortModePreference(Context context, RouteListType listType, RouteSortMode sortMode) {
        try {
            Shared.Companion.getRouteSortModePreference().put(listType, sortMode);
            PREFERENCES.getRouteSortModePreference().clear();
            PREFERENCES.getRouteSortModePreference().putAll(Shared.Companion.getRouteSortModePreference());
            savePreferences(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelCurrentChecksumTask() {
        Future<?> task = currentChecksumTask.get();
        if (task != null) {
            task.cancel(true);
        }
    }

    private void ensureData(Context context, boolean suppressUpdateCheck) throws IOException {
        if (state.getValue() == State.READY) {
            return;
        }
        if (PREFERENCES != null && DATA != null) {
            return;
        }

        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8))) {
                PREFERENCES = Preferences.deserialize(new JSONObject(reader.lines().collect(Collectors.joining())));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (PREFERENCES == null) {
            PREFERENCES = Preferences.createDefault();
            try {
                savePreferences(context);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Shared.Companion.setLanguage(PREFERENCES.getLanguage());
        Shared.Companion.updateFavoriteRouteStops(m -> {
            m.clear();
            m.putAll(PREFERENCES.getFavouriteRouteStops());
        });
        Shared.Companion.updateEtaTileConfigurations(m -> {
            m.clear();
            m.putAll(PREFERENCES.getEtaTileConfigurations());
        });
        Shared.Companion.clearLookupRoute();
        List<LastLookupRoute> lastLookupRoutes = PREFERENCES.getLastLookupRoutes();
        for (Iterator<LastLookupRoute> itr = lastLookupRoutes.iterator(); itr.hasNext();) {
            LastLookupRoute lastLookupRoute = itr.next();
            if (lastLookupRoute.isValid()) {
                Shared.Companion.addLookupRoute(lastLookupRoute);
            } else {
                itr.remove();
            }
        }
        Shared.Companion.getRouteSortModePreference().clear();
        Shared.Companion.getRouteSortModePreference().putAll(PREFERENCES.getRouteSortModePreference());

        checkUpdate(context, suppressUpdateCheck);
    }

    public long getLastUpdateCheck() {
        return lastUpdateCheck.get();
    }

    public void checkUpdate(Context context, boolean suppressUpdateCheck) {
        state.setValue(State.LOADING);
        if (!suppressUpdateCheck) {
            lastUpdateCheck.set(System.currentTimeMillis());
        }
        new Thread(() -> {
            try {
                List<String> files = Arrays.asList(context.getApplicationContext().fileList());
                ConnectionUtils.ConnectionType connectionType = ConnectionUtils.getConnectionType(context);

                AtomicBoolean updateChecked = new AtomicBoolean(false);
                Function<Boolean, String> checksumFetcher = forced -> {
                    FutureTask<String> future = new FutureTask<>(() -> {
                        long version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
                        return HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/checksum.md5") + "_" + version;
                    });
                    currentChecksumTask.set(future);
                    if (!forced && files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                        state.setValue(State.UPDATE_CHECKING);
                    }
                    try {
                        new Thread(future).start();
                        String result = future.get(10, TimeUnit.SECONDS);
                        updateChecked.set(true);
                        return result;
                    } catch (ExecutionException | TimeoutException | InterruptedException | CancellationException e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        if (state.getValue() == State.UPDATE_CHECKING) {
                            state.setValue(State.LOADING);
                        }
                    }
                };

                boolean cached = false;
                String checksum = !suppressUpdateCheck && connectionType.hasConnection() ? checksumFetcher.apply(false) : null;
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
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
                    if (DATA == null) {
                        try {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(DATA_FILE_NAME), StandardCharsets.UTF_8))) {
                                DATA = DataContainer.deserialize(new JSONObject(reader.lines().collect(Collectors.joining())));
                            }
                            updateTileService(context);
                            state.setValue(State.READY);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    } else {
                        state.setValue(State.READY);
                    }
                }
                if (state.getValue() != State.READY) {
                    if (!connectionType.hasConnection()) {
                        state.setValue(State.ERROR);
                        try { context.getApplicationContext().deleteFile(CHECKSUM_FILE_NAME); } catch (Throwable ignore) {}
                    } else {
                        state.setValue(State.UPDATING);
                        updatePercentageState.setValue(0F);
                        float percentageOffset = Shared.Companion.getFavoriteRouteStops().isEmpty() ? 0.15F : 0F;

                        if (!updateChecked.get()) {
                            checksum = checksumFetcher.apply(true);
                        }

                        long length = IntUtils.parseOr(HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/size.gz.dat"), -1);
                        String textResponse = HTTPRequestUtils.getTextResponseWithPercentageCallback("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/data.json.gz", length, GZIPInputStream::new, p -> updatePercentageState.setValue(p * 0.75F + percentageOffset));
                        if (textResponse == null) {
                            throw new RuntimeException("Error downloading bus data");
                        }

                        DATA = DataContainer.deserialize(new JSONObject(textResponse));
                        updatePercentageState.setValue(0.75F + percentageOffset);

                        AtomicFile atomicDataFile = new AtomicFile(context.getApplicationContext().getFileStreamPath(DATA_FILE_NAME));
                        try (FileOutputStream fos = atomicDataFile.startWrite();
                             PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                            pw.write(textResponse);
                            pw.flush();
                            atomicDataFile.finishWrite(fos);
                        }
                        updatePercentageState.setValue(0.825F + percentageOffset);
                        AtomicFile atomicChecksumFile = new AtomicFile(context.getApplicationContext().getFileStreamPath(CHECKSUM_FILE_NAME));
                        try (FileOutputStream fos = atomicChecksumFile.startWrite();
                             PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                            pw.write(checksum == null ? "" : checksum);
                            pw.flush();
                            atomicChecksumFile.finishWrite(fos);
                        }
                        updatePercentageState.setValue(0.85F + percentageOffset);

                        float localUpdatePercentage = updatePercentageState.getValue();
                        float percentagePerFav = 0.15F / Shared.Companion.getFavoriteRouteStops().size();
                        List<Runnable> updatedFavouriteRouteTasks = new ArrayList<>();
                        for (Map.Entry<Integer, FavouriteRouteStop> entry : Shared.Companion.getFavoriteRouteStops().entrySet()) {
                            try {
                                int favouriteRouteIndex = entry.getKey();
                                FavouriteRouteStop favouriteRoute = entry.getValue();

                                Route oldRoute = favouriteRoute.getRoute();
                                String stopId = favouriteRoute.getStopId();
                                Operator co = favouriteRoute.getCo();

                                List<RouteSearchResultEntry> newRoutes = findRoutes(oldRoute.getRouteNumber(), true, r -> {
                                    if (!r.getBound().containsKey(co)) {
                                        return false;
                                    }
                                    if (co == Operator.GMB) {
                                        if (!Objects.equals(r.getGmbRegion(), oldRoute.getGmbRegion())) {
                                            return false;
                                        }
                                    } else if (co == Operator.NLB) {
                                        return r.getNlbId().equals(oldRoute.getNlbId());
                                    }
                                    return r.getBound().get(co).equals(oldRoute.getBound().get(co));
                                });

                                if (newRoutes.isEmpty()) {
                                    updatedFavouriteRouteTasks.add(() -> clearFavouriteRouteStop(favouriteRouteIndex, false, context));
                                    continue;
                                }
                                RouteSearchResultEntry newRouteData = newRoutes.get(0);
                                Route newRoute = newRouteData.getRoute();
                                List<StopData> stopList = getAllStops(
                                        newRoute.getRouteNumber(),
                                        co == Operator.NLB ? newRoute.getNlbId() : newRoute.getBound().get(co),
                                        co,
                                        newRoute.getGmbRegion()
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
                                updatedFavouriteRouteTasks.add(() -> setFavouriteRouteStop(favouriteRouteIndex, finalStopId, co, finalIndex, stop, stopData.getRoute(), true, false, context));
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            localUpdatePercentage += percentagePerFav;
                            updatePercentageState.setValue(localUpdatePercentage);
                        }
                        if (!updatedFavouriteRouteTasks.isEmpty()) {
                            updatedFavouriteRouteTasks.forEach(Runnable::run);
                            savePreferences(context);
                        }
                        updatePercentageState.setValue(1F);

                        updateTileService(context);
                        state.setValue(State.READY);
                    }
                }
                updatePercentageState.setValue(1F);
            } catch (Exception e) {
                e.printStackTrace();
                state.setValue(State.ERROR);
            }
            if (state.getValue() != State.READY) {
                state.setValue(State.ERROR);
            }
        }).start();
    }

    public String getRouteKey(Route route) {
        return DATA.getDataSheet().getRouteList().entrySet().stream().filter(e -> e.getValue().equals(route)).findFirst().map(Map.Entry::getKey).orElse(null);
    }

    public Route findRouteByKey(String inputKey, String routeNumber) {
        Route exact = DATA.getDataSheet().getRouteList().get(inputKey);
        if (exact != null) {
            return exact;
        }
        inputKey = inputKey.toLowerCase();
        Route nearestRoute = null;
        int distance = Integer.MAX_VALUE;
        for (Map.Entry<String, Route> entry : DATA.getDataSheet().getRouteList().entrySet()) {
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
        return DATA.getDataSheet().getStopList().get(stopId);
    }

    public PossibleNextCharResult getPossibleNextChar(String input) {
        Set<Character> result = new HashSet<>();
        boolean exactMatch = false;
        for (String routeNumber : DATA.getBusRoute()) {
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

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact, BiPredicate<Route, Operator> coPredicate) {
        return findRoutes(input, exact, r -> true, coPredicate);
    }

    public List<RouteSearchResultEntry> findRoutes(String input, boolean exact, Predicate<Route> predicate, BiPredicate<Route, Operator> coPredicate) {
        Predicate<String> routeMatcher = exact ? r -> r.equals(input) : r -> r.startsWith(input);
        Map<String, RouteSearchResultEntry> matchingRoutes = new HashMap<>();

        for (Map.Entry<String, Route> entry : DATA.getDataSheet().getRouteList().entrySet()) {
            String key = entry.getKey();
            Route data = entry.getValue();
            if (data.isCtbIsCircular()) {
                continue;
            }
            if (routeMatcher.test(data.getRouteNumber()) && predicate.test(data)) {
                Operator co;
                Map<Operator, String> bound = data.getBound();
                if (bound.containsKey(Operator.KMB)) {
                    co = Operator.KMB;
                } else if (bound.containsKey(Operator.CTB)) {
                    co = Operator.CTB;
                } else if (bound.containsKey(Operator.NLB)) {
                    co = Operator.NLB;
                } else if (bound.containsKey(Operator.MTR_BUS)) {
                    co = Operator.MTR_BUS;
                } else if (bound.containsKey(Operator.GMB)) {
                    co = Operator.GMB;
                } else if (bound.containsKey(Operator.LRT)) {
                    co = Operator.LRT;
                } else if (bound.containsKey(Operator.MTR)) {
                    co = Operator.MTR;
                } else {
                    continue;
                }
                if (!coPredicate.test(data, co)) {
                    continue;
                }
                String key0 = data.getRouteNumber() + "," + co.name() + "," + (co == Operator.NLB ? data.getNlbId() : data.getBound().get(co)) + (co == Operator.GMB ? ("," + data.getGmbRegion()) : "");

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

        return matchingRoutes.values().stream().sorted((a, b) -> {
            Route routeA = a.getRoute();
            Route routeB = b.getRoute();

            Map<Operator, String> boundA = routeA.getBound();
            Map<Operator, String> boundB = routeB.getBound();

            Operator coA = boundA.keySet().stream().max(Comparator.naturalOrder()).orElseThrow(NoSuchElementException::new);
            Operator coB = boundB.keySet().stream().max(Comparator.naturalOrder()).orElseThrow(NoSuchElementException::new);
            int coDiff = coA.compareTo(coB);
            if (coDiff != 0) {
                return coDiff;
            }

            String routeNumberA = routeA.getRouteNumber();
            String routeNumberB = routeB.getRouteNumber();

            if (RouteExtensionsKt.isTrain(coA) && RouteExtensionsKt.isTrain(coB)) {
                int lineDiff = Integer.compare(Shared.Companion.getMtrLineSortingIndex(routeNumberA), Shared.Companion.getMtrLineSortingIndex(routeNumberB));
                if (lineDiff != 0) {
                    return lineDiff;
                }
                return -boundA.get(coA).compareTo(boundB.get(coB));
            } else {
                int routeNumberDiff = routeNumberA.compareTo(routeNumberB);
                if (routeNumberDiff != 0) {
                    return routeNumberDiff;
                }
            }
            if (coA == Operator.NLB) {
                return IntUtils.parseOrZero(routeA.getNlbId()) - IntUtils.parseOrZero(routeB.getNlbId());
            }
            if (coA == Operator.GMB) {
                int gtfsDiff = IntUtils.parseOrZero(routeA.getGtfsId()) - IntUtils.parseOrZero(routeB.getGtfsId());
                if (gtfsDiff != 0) {
                    return gtfsDiff;
                }
            }
            int typeDiff = IntUtils.parseOrZero(routeA.getServiceType()) - IntUtils.parseOrZero(routeB.getServiceType());
            if (typeDiff == 0) {
                if (coA == Operator.CTB) {
                    return 0;
                }
                return -boundA.get(coA).compareTo(boundB.get(coB));
            }
            return typeDiff;
        }).collect(Collectors.toList());
    }

    public NearbyRoutesResult getNearbyRoutes(double lat, double lng, Set<String> excludedRouteNumbers, boolean isInterchangeSearch) {
        Coordinates origin = new Coordinates(lat, lng);

        Map<String, Stop> stops = DATA.getDataSheet().getStopList();
        List<RouteSearchResultEntry.StopInfo> nearbyStops = new ArrayList<>();

        Stop closestStop = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Stop> stopEntry : stops.entrySet()) {
            String stopId = stopEntry.getKey();
            Stop entry = stopEntry.getValue();
            Coordinates location = entry.getLocation();
            double distance = DistanceUtils.findDistance(lat, lng, location.getLat(), location.getLng());

            if (distance < closestDistance) {
                closestStop = entry;
                closestDistance = distance;
            }

            if (distance <= 0.3) {
                Operator co = RouteExtensionsKt.identifyStopCo(stopId);
                if (co == null) {
                    continue;
                }
                nearbyStops.add(new RouteSearchResultEntry.StopInfo(stopId, entry, distance, co));
            }
        }

        Map<String, RouteSearchResultEntry> nearbyRoutes = new HashMap<>();

        for (RouteSearchResultEntry.StopInfo nearbyStop : nearbyStops) {
            String stopId = nearbyStop.getStopId();

            for (Map.Entry<String, Route> entry : DATA.getDataSheet().getRouteList().entrySet()) {
                String key = entry.getKey();
                Route data = entry.getValue();

                if (excludedRouteNumbers.contains(data.getRouteNumber())) {
                    continue;
                }
                if (data.isCtbIsCircular()) {
                    continue;
                }

                Operator co = Arrays.stream(Operator.values()).filter(c -> {
                    if (!data.getBound().containsKey(c)) {
                        return false;
                    }
                    List<String> coStops = data.getStops().get(c);
                    if (coStops == null) {
                        return false;
                    }
                    return coStops.contains(stopId);
                }).findFirst().orElse(null);
                if (co != null) {
                    String key0 = data.getRouteNumber() + "," + co.name() + "," + (co == Operator.NLB ? data.getNlbId() : data.getBound().get(co)) + (co == Operator.GMB ? ("," + data.getGmbRegion()) : "");

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

        ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
        int hour = hongKongTime.getHour();
        boolean isNight = hour >= 1 && hour < 5;
        DayOfWeek weekday = hongKongTime.getDayOfWeek();
        LocalDate date = hongKongTime.toLocalDate();

        boolean isHoliday = weekday.equals(DayOfWeek.SATURDAY) || weekday.equals(DayOfWeek.SUNDAY) || DATA.getDataSheet().getHolidays().contains(date);

        return new NearbyRoutesResult(CollectionsUtilsKtKt.distinctBy(nearbyRoutes.values().stream().sorted(Comparator.comparing((RouteSearchResultEntry a) -> {
            Route route = a.getRoute();
            String routeNumber = route.getRouteNumber();
            Map<Operator, String> bound = route.getBound();

            String pa = String.valueOf(routeNumber.charAt(0));
            String sa = String.valueOf(routeNumber.charAt(routeNumber.length() - 1));
            int na = IntUtils.parseOrZero(routeNumber.replaceAll("[^0-9]", ""));

            if (bound.containsKey(Operator.GMB)) {
                na += 1000;
            } else if (bound.containsKey(Operator.MTR)) {
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
            return a.getCo();
        }).thenComparing(Comparator.comparing((RouteSearchResultEntry a) -> {
            Route route = a.getRoute();
            Map<Operator, String> bound = route.getBound();
            if (bound.containsKey(Operator.MTR)) {
                return Shared.Companion.getMtrLineSortingIndex(route.getRouteNumber());
            }
            return 10;
        }).reversed())), RouteSearchResultEntry::getRouteKey).collect(Collectors.toList()), closestStop, closestDistance, lat, lng);
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

    public List<StopData> getAllStops(String routeNumber, String bound, Operator co, GMBRegion gmbRegion) {
        try {
            List<Pair<BranchedList<String, StopData>, Integer>> lists = new ArrayList<>();
            for (Route route : DATA.getDataSheet().getRouteList().values()) {
                if (routeNumber.equals(route.getRouteNumber()) && route.getCo().contains(co)) {
                    boolean flag;
                    if (co == Operator.NLB) {
                        flag = bound.equals(route.getNlbId());
                    } else {
                        flag = bound.equals(route.getBound().get(co));
                        if (co == Operator.GMB) {
                            flag &= Objects.equals(gmbRegion, route.getGmbRegion());
                        }
                    }
                    if (flag) {
                        BranchedList<String, StopData> localStops = new BranchedList<>();
                        List<String> stops = route.getStops().get(co);
                        int serviceType = IntUtils.parseOr(route.getServiceType(), 1);
                        for (String stopId : stops) {
                            localStops.add(stopId, new StopData(stopId, serviceType, DATA.getDataSheet().getStopList().get(stopId), route));
                        }
                        lists.add(new Pair<>(localStops, serviceType));
                    }
                }
            }
            lists.sort(Comparator.comparing(Pair<BranchedList<String, StopData>, Integer>::getSecond));
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
                result.merge(pair.getFirst());
            }
            return result.valuesWithBranchIds().stream().map(p -> p.getFirst().withBranchIndex(p.getSecond())).collect(Collectors.toList());
        } catch (Throwable e) {
            throw new RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gmbRegion + ": " + e.getMessage(), e);
        }
    }

    @Immutable
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

    public Pair<List<BilingualText>, List<BilingualText>> getAllOriginsAndDestinations(String routeNumber, String bound, Operator co, GMBRegion gmbRegion) {
        try {
            List<Triple<BilingualText, Integer, String>> origs = new ArrayList<>();
            List<Pair<BilingualText, Integer>> dests = new ArrayList<>();
            int mainRouteServiceType = Integer.MAX_VALUE;
            List<String> mainRouteStops = Collections.emptyList();
            for (Route route : DATA.getDataSheet().getRouteList().values()) {
                if (routeNumber.equals(route.getRouteNumber()) && route.getCo().contains(co)) {
                    boolean flag;
                    if (co == Operator.NLB) {
                        flag = bound.equals(route.getNlbId());
                    } else {
                        flag = bound.equals(route.getBound().get(co));
                        if (co == Operator.GMB) {
                            flag &= Objects.equals(gmbRegion, route.getGmbRegion());
                        }
                    }
                    if (flag) {
                        int serviceType = IntUtils.parseOr(route.getServiceType(), 1);
                        List<String> stops = route.getStops().get(co);
                        if (mainRouteServiceType > serviceType || (mainRouteServiceType == serviceType && stops.size() > mainRouteStops.size())) {
                            mainRouteServiceType = serviceType;
                            mainRouteStops = stops;
                        }
                        BilingualText orig = route.getOrig();
                        Triple<BilingualText, Integer, String> oldOrig = origs.stream().filter(d -> d.getFirst().getZh().equals(orig.getZh())).findFirst().orElse(null);
                        if (oldOrig == null || oldOrig.getSecond() > serviceType) {
                            origs.add(new Triple<>(orig, serviceType, stops.get(0)));
                        }
                        BilingualText dest = route.getDest();
                        Pair<BilingualText, Integer> oldDest = dests.stream().filter(d -> d.getFirst().getZh().equals(dest.getZh())).findFirst().orElse(null);
                        if (oldDest == null || oldDest.getSecond() > serviceType) {
                            dests.add(new Pair<>(dest, serviceType));
                        }
                    }
                }
            }
            List<String> finalMainRouteStops = mainRouteStops;
            return new Pair<>(
                    origs.stream().filter(p -> !finalMainRouteStops.contains(p.getThird())).sorted(Comparator.comparing(p -> p.getSecond())).map(p -> p.getFirst()).collect(Collectors.toList()),
                    dests.stream().sorted(Comparator.comparing(p -> p.getSecond())).map(p -> p.getFirst()).collect(Collectors.toList())
            );
        } catch (Throwable e) {
            throw new RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gmbRegion + ": " + e.getMessage(), e);
        }
    }

    public BilingualText getStopSpecialDestinations(String stopId, Operator co, Route route, boolean prependTo) {
        if (route.getLrtCircular() != null) {
            return route.getLrtCircular();
        }
        String bound = route.getBound().get(co);
        switch (stopId) {
            case "LHP": {
                if (bound.contains("UT")) {
                    return prependTo(new BilingualText("康城", "LOHAS Park"));
                } else {
                    return prependTo(new BilingualText("北角/寶琳", "North Point/Po Lam"));
                }
            }
            case "HAH":
            case "POA": {
                if (bound.contains("UT")) {
                    return prependTo(new BilingualText("寶琳", "Po Lam"));
                } else {
                    return prependTo(new BilingualText("北角/康城", "North Point/LOHAS Park"));
                }
            }
            case "AIR":
            case "AWE": {
                if (bound.contains("UT")) {
                    return prependTo(new BilingualText("博覽館", "AsiaWorld-Expo"));
                }
            }
        }
        return prependTo(route.getDest());
    }

    public boolean isLrtStopOnOrAfter(String thisStopId, String targetStopNameZh, Route route) {
        if (route.getLrtCircular() != null && targetStopNameZh.equals(route.getLrtCircular().getZh())) {
            return true;
        }
        List<String> stopIds = route.getStops().get(Operator.LRT);
        if (stopIds == null) {
            return false;
        }
        int stopIndex = stopIds.indexOf(thisStopId);
        if (stopIndex < 0) {
            return false;
        }
        for (int i = stopIndex; i < stopIds.size(); i++) {
            String stopId = stopIds.get(i);
            Stop stop = DATA.getDataSheet().getStopList().get(stopId);
            if (stop != null && stop.getName().getZh().equals(targetStopNameZh)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMtrStopOnOrAfter(String stopId, String relativeTo, String lineName, String bound) {
        for (Route data : DATA.getDataSheet().getRouteList().values()) {
            if (lineName.equals(data.getRouteNumber()) && data.getBound().get(Operator.MTR).endsWith(bound)) {
                List<String> stopsList = data.getStops().get(Operator.MTR);
                int index = stopsList.indexOf(stopId);
                int indexRef = stopsList.indexOf(relativeTo);
                if (indexRef >= 0 && index >= indexRef) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMtrStopEndOfLine(String stopId, String lineName, String bound) {
        for (Route data : DATA.getDataSheet().getRouteList().values()) {
            if (lineName.equals(data.getRouteNumber()) && data.getBound().get(Operator.MTR).endsWith(bound)) {
                List<String> stopsList = data.getStops().get(Operator.MTR);
                int index = stopsList.indexOf(stopId);
                if (index >= 0 && index + 1 < stopsList.size()) {
                    return false;
                }
            }
        }
        return true;
    }

    public MTRInterchangeData getMtrStationInterchange(String stopId, String lineName) {
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
        String stopName = DATA.getDataSheet().getStopList().get(stopId).getName().getZh();
        for (Route data : DATA.getDataSheet().getRouteList().values()) {
            Map<Operator, String> bound = data.getBound();
            String routeNumber = data.getRouteNumber();
            if (!routeNumber.equals(lineName)) {
                if (bound.containsKey(Operator.MTR)) {
                    List<String> stopsList = data.getStops().get(Operator.MTR).stream().map(id -> DATA.getDataSheet().getStopList().get(id).getName().getZh()).collect(Collectors.toList());
                    if (stopsList.contains(stopName)) {
                        lines.add(routeNumber);
                    } else if (outOfStationStopName != null && stopsList.contains(outOfStationStopName)) {
                        outOfStationLines.add(routeNumber);
                    }
                } else if (bound.containsKey(Operator.LRT) && !hasLightRail) {
                    if (data.getStops().get(Operator.LRT).stream().anyMatch(id -> DATA.getDataSheet().getStopList().get(id).getName().getZh().equals(stopName))) {
                        hasLightRail = true;
                    }
                }
            }
        }
        return new MTRInterchangeData(lines, isOutOfStationPaid, outOfStationLines, hasLightRail);
    }

    @Immutable
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

    public String getNoScheduledDepartureMessage(String altMessage, boolean isAboveTyphoonSignalEight, String typhoonWarningTitle) {
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

    public StateFlow<TyphoonInfo> getCachedTyphoonDataState() {
        return typhoonInfo;
    }

    public Future<TyphoonInfo> getCurrentTyphoonData() {
        TyphoonInfo cache = typhoonInfo.getValue();
        if (cache != null && System.currentTimeMillis() - cache.getLastUpdated() < 300000) {
            return CompletableFuture.completedFuture(cache);
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
                    TyphoonInfo info = TyphoonInfo.info(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId);
                    typhoonInfo.setValue(info);
                    future.complete(info);
                    return;
                }
            }
            TyphoonInfo info = TyphoonInfo.none();
            typhoonInfo.setValue(info);
            future.complete(info);
        }).start();
        return future;
    }

    @Immutable
    public static class TyphoonInfo {

        public static final TyphoonInfo NULL = new TyphoonInfo(false, false, "", "", 0);

        public static TyphoonInfo none() {
            return new TyphoonInfo(false, false, "", "", System.currentTimeMillis());
        }

        public static TyphoonInfo info(boolean isAboveTyphoonSignalEight, boolean isAboveTyphoonSignalNine, String typhoonWarningTitle, String currentTyphoonSignalId) {
            return new TyphoonInfo(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId, System.currentTimeMillis());
        }

        private final boolean isAboveTyphoonSignalEight;
        private final boolean isAboveTyphoonSignalNine;
        private final String typhoonWarningTitle;
        private final String currentTyphoonSignalId;
        private final long lastUpdated;

        private TyphoonInfo(boolean isAboveTyphoonSignalEight, boolean isAboveTyphoonSignalNine, String typhoonWarningTitle, String currentTyphoonSignalId, long lastUpdated) {
            this.isAboveTyphoonSignalEight = isAboveTyphoonSignalEight;
            this.isAboveTyphoonSignalNine = isAboveTyphoonSignalNine;
            this.typhoonWarningTitle = typhoonWarningTitle;
            this.currentTyphoonSignalId = currentTyphoonSignalId;
            this.lastUpdated = lastUpdated;
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

        public long getLastUpdated() {
            return lastUpdated;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TyphoonInfo info = (TyphoonInfo) o;
            return isAboveTyphoonSignalEight == info.isAboveTyphoonSignalEight && isAboveTyphoonSignalNine == info.isAboveTyphoonSignalNine && lastUpdated == info.lastUpdated && Objects.equals(typhoonWarningTitle, info.typhoonWarningTitle) && Objects.equals(currentTyphoonSignalId, info.currentTyphoonSignalId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId, lastUpdated);
        }
    }

    public PendingETAQueryResult getEta(String stopId, int stopIndex, Operator co, Route route, Context context) {
        Bundle bundle = new Bundle();
        bundle.putString("route", stopId + "," + stopIndex + "," + route.getRouteNumber() + "," + co.name() + "," + route.getBound().get(co));
        bundle.putString("stop", route.getRouteNumber() + "," + co.name() + "," + route.getBound().get(co));
        FirebaseAnalytics.getInstance(context).logEvent("eta_query", bundle);
        PendingETAQueryResult pending = new PendingETAQueryResult(context, co, () -> {
            TyphoonInfo typhoonInfo = getCurrentTyphoonData().get();

            Map<Integer, ETALineEntry> lines = new HashMap<>();
            boolean isMtrEndOfLine = false;
            boolean isTyphoonSchedule = false;
            Operator nextCo = co;

            lines.put(1, ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle())));
            String language = Shared.Companion.getLanguage();
            if (route.isKmbCtbJoint()) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();
                String dest = route.getDest().getZh().replace(" ", "");
                String orig = route.getOrig().getZh().replace(" ", "");
                Set<JointOperatedEntry> jointOperated = ConcurrentHashMap.newKeySet();
                AtomicReference<String> kmbSpecialMessage = new AtomicReference<>(null);
                AtomicLong kmbFirstScheduledBus = new AtomicLong(Long.MAX_VALUE);
                Future<?> kmbFuture = ETA_QUERY_EXECUTOR.submit(() -> {
                    JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/" + stopId);
                    JSONArray buses = data.optJSONArray("data");

                    Set<Integer> stopSequences = new HashSet<>();
                    for (int u = 0; u < buses.length(); u++) {
                        JSONObject bus = buses.optJSONObject(u);
                        if (Operator.KMB == Operator.valueOf(bus.optString("co"))) {
                            String routeNumber = bus.optString("route");
                            String bound = bus.optString("dir");
                            if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get(Operator.KMB))) {
                                stopSequences.add(bus.optInt("seq"));
                            }
                        }
                    }
                    int matchingSeq = stopSequences.stream().min(Comparator.comparing(i -> Math.abs(i - stopIndex))).orElse(-1);
                    Set<Integer> usedRealSeq = new HashSet<>();
                    for (int u = 0; u < buses.length(); u++) {
                        JSONObject bus = buses.optJSONObject(u);
                        if (Operator.KMB == Operator.valueOf(bus.optString("co"))) {
                            String routeNumber = bus.optString("route");
                            String bound = bus.optString("dir");
                            int stopSeq = bus.optInt("seq");
                            if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get(Operator.KMB)) && stopSeq == matchingSeq && usedRealSeq.add(bus.optInt("eta_seq"))) {
                                String eta = bus.optString("eta");
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                if (!eta.isEmpty() && !eta.equalsIgnoreCase("null")) {
                                    double mins = (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                                    long minsRounded = Math.round(mins);
                                    String message = "";
                                    if (language.equals("en")) {
                                        if (minsRounded > 0) {
                                            message = "<b>" + minsRounded + "</b><small> Min.</small>";
                                        } else if (minsRounded > -60) {
                                            message = "<b>-</b><small> Min.</small>";
                                        }
                                        if (!bus.optString("rmk_en").isEmpty()) {
                                            message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                        }
                                    } else {
                                        if (minsRounded > 0) {
                                            message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                                        } else if (minsRounded > -60) {
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
                                    if ((message.contains("預定班次") || message.contains("Scheduled Bus")) && mins < kmbFirstScheduledBus.get()) {
                                        kmbFirstScheduledBus.set(minsRounded);
                                    }
                                    jointOperated.add(new JointOperatedEntry(mins, minsRounded, message, Operator.KMB));
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
                                    kmbSpecialMessage.set(message);
                                }
                            }
                        }
                    }
                });
                {
                    String routeNumber = route.getRouteNumber();
                    List<Pair<Operator, String>> matchingStops = DATA.getDataSheet().getStopMap().get(stopId);
                    List<String> ctbStopIds = new ArrayList<>();
                    if (matchingStops != null) {
                        for (Pair<Operator, String> stopArray : matchingStops) {
                            if (Operator.CTB == stopArray.getFirst()) {
                                ctbStopIds.add(stopArray.getSecond());
                            }
                        }
                    }
                    Set<String> destKeys = Set.of(dest, orig);
                    Map<String, Set<JointOperatedEntry>> ctbEtaEntries = new ConcurrentHashMap<>();
                    List<Future<?>> ctbFutures = new ArrayList<>(ctbStopIds.size());
                    for (String ctbStopId : ctbStopIds) {
                        ctbFutures.add(ETA_QUERY_EXECUTOR.submit(() -> {
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/" + ctbStopId + "/" + routeNumber);
                            JSONArray buses = data.optJSONArray("data");
                            Map<String, Set<Integer>> stopSequences = new HashMap<>();
                            String[] busDests = new String[buses.length()];
                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if (Operator.CTB == Operator.valueOf(bus.optString("co")) && routeNumber.equals(bus.optString("route"))) {
                                    String rawBusDest = bus.optString("dest_tc").replace(" ", "");
                                    String busDest = destKeys.stream()
                                            .min(Comparator.comparing(d -> StringUtils.editDistance(d, rawBusDest)))
                                            .orElseThrow(RuntimeException::new);
                                    busDests[u] = busDest;
                                    stopSequences.computeIfAbsent(busDest, k -> new HashSet<>()).add(bus.optInt("seq"));
                                }
                            }
                            Map<String, Integer> matchingSeq = stopSequences.entrySet().stream()
                                    .map(e -> new Pair<>(e.getKey(), e.getValue().stream().min(Comparator.comparing(i -> Math.abs(i - stopIndex))).orElse(-1)))
                                    .collect(Collectors.toMap(Pair<String, Integer>::getFirst, Pair<String, Integer>::getSecond));
                            Map<String, Set<Integer>> usedRealSeq = new HashMap<>();
                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if (Operator.CTB == Operator.valueOf(bus.optString("co")) && routeNumber.equals(bus.optString("route"))) {
                                    String busDest = busDests[u];
                                    int stopSeq = bus.optInt("seq");
                                    if (stopSeq == matchingSeq.getOrDefault(busDest, 0) && usedRealSeq.computeIfAbsent(busDest, k -> new HashSet<>()).add(bus.optInt("eta_seq"))) {
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        if (!eta.isEmpty() && !eta.equalsIgnoreCase("null")) {
                                            double mins = (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                                            long minsRounded = Math.round(mins);
                                            String message = "";
                                            if (language.equals("en")) {
                                                if (minsRounded > 0) {
                                                    message = "<b>" + minsRounded + "</b><small> Min.</small>";
                                                } else if (minsRounded > -60) {
                                                    message = "<b>-</b><small> Min.</small>";
                                                }
                                                if (!bus.optString("rmk_en").isEmpty()) {
                                                    message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                                }
                                            } else {
                                                if (minsRounded > 0) {
                                                    message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                                                } else if (minsRounded > -60) {
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
                                            ctbEtaEntries.computeIfAbsent(busDest, k -> ConcurrentHashMap.newKeySet()).add(new JointOperatedEntry(mins, minsRounded, message, Operator.CTB));
                                        }
                                    }
                                }
                            }
                        }));
                    }
                    for (Future<?> future : ctbFutures) {
                        future.get();
                    }
                    Set<JointOperatedEntry> entries = ctbEtaEntries.get(dest);
                    if (entries != null) {
                        jointOperated.addAll(entries);
                    }
                }
                kmbFuture.get();

                if (jointOperated.isEmpty()) {
                    if (kmbSpecialMessage.get() == null || kmbSpecialMessage.get().isEmpty()) {
                        lines.put(1, ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle())));
                    } else {
                        lines.put(1, ETALineEntry.textEntry(kmbSpecialMessage.get()));
                    }
                } else {
                    int counter = 0;
                    for (Iterator<JointOperatedEntry> itr = jointOperated.stream().sorted().iterator(); itr.hasNext(); ) {
                        JointOperatedEntry entry = itr.next();
                        double mins = entry.getMins();
                        long minsRounded = entry.getMinsRounded();

                        String message = "<b></b>" + entry.getLine().replace("(尾班車)", "").replace("(Final Bus)", "").trim();
                        Operator entryCo = entry.getCo();
                        if (minsRounded > kmbFirstScheduledBus.get() && !(message.contains("預定班次") || message.contains("Scheduled Bus"))) {
                            message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " (Scheduled Bus)" : " (預定班次)") + "</small>";
                        }
                        if (entryCo == Operator.KMB) {
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
                            nextCo = entryCo;
                        }
                        lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                    }
                }
            } else if (co == Operator.KMB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/" + stopId);
                JSONArray buses = data.optJSONArray("data");

                Set<Integer> stopSequences = new HashSet<>();
                for (int u = 0; u < buses.length(); u++) {
                    JSONObject bus = buses.optJSONObject(u);
                    if (Operator.KMB == Operator.valueOf(bus.optString("co"))) {
                        String routeNumber = bus.optString("route");
                        String bound = bus.optString("dir");
                        if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get(Operator.KMB))) {
                            stopSequences.add(bus.optInt("seq"));
                        }
                    }
                }
                int matchingSeq = stopSequences.stream().min(Comparator.comparing(i -> Math.abs(i - stopIndex))).orElse(-1);
                int counter = 0;
                Set<Integer> usedRealSeq = new HashSet<>();
                for (int u = 0; u < buses.length(); u++) {
                    JSONObject bus = buses.optJSONObject(u);
                    if (Operator.KMB == Operator.valueOf(bus.optString("co"))) {
                        String routeNumber = bus.optString("route");
                        String bound = bus.optString("dir");
                        int stopSeq = bus.optInt("seq");
                        if (routeNumber.equals(route.getRouteNumber()) && bound.equals(route.getBound().get(Operator.KMB)) && stopSeq == matchingSeq) {
                            int seq = ++counter;
                            if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                String eta = bus.optString("eta");
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                double mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                                long minsRounded = Math.round(mins);
                                String message = "";
                                if (language.equals("en")) {
                                    if (minsRounded > 0) {
                                        message = "<b>" + minsRounded + "</b><small> Min.</small>";
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> Min.</small>";
                                    }
                                    if (!bus.optString("rmk_en").isEmpty()) {
                                        message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                                    } else if (minsRounded > -60) {
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

                                if (message.isEmpty() || (typhoonInfo.isAboveTyphoonSignalEight() && (message.equals("ETA service suspended") || message.equals("暫停預報")))) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                    } else {
                                        message = "<b></b>-";
                                    }
                                } else {
                                    message = "<b></b>" + message;
                                }
                                lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                            }
                        }
                    }
                }
            } else if (co == Operator.CTB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                String routeNumber = route.getRouteNumber();
                String routeBound = route.getBound().get(Operator.CTB);
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/" + stopId + "/" + routeNumber);

                JSONArray buses = data.optJSONArray("data");
                Set<Integer> stopSequences = new HashSet<>();
                for (int u = 0; u < buses.length(); u++) {
                    JSONObject bus = buses.optJSONObject(u);
                    if (Operator.CTB == Operator.valueOf(bus.optString("co"))) {
                        String bound = bus.optString("dir");
                        if (routeNumber.equals(bus.optString("route")) && (routeBound.length() > 1 || bound.equals(routeBound))) {
                            stopSequences.add(bus.optInt("seq"));
                        }
                    }
                }
                int matchingSeq = stopSequences.stream().min(Comparator.comparing(i -> Math.abs(i - stopIndex))).orElse(-1);
                int counter = 0;
                Set<Integer> usedRealSeq = new HashSet<>();
                for (int u = 0; u < buses.length(); u++) {
                    JSONObject bus = buses.optJSONObject(u);
                    if (Operator.CTB == Operator.valueOf(bus.optString("co"))) {
                        String bound = bus.optString("dir");
                        int stopSeq = bus.optInt("seq");
                        if (routeNumber.equals(bus.optString("route")) && (routeBound.length() > 1 || bound.equals(routeBound)) && stopSeq == matchingSeq) {
                            int seq = ++counter;
                            if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                String eta = bus.optString("eta");
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                double mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                                long minsRounded = Math.round(mins);
                                String message = "";
                                if (language.equals("en")) {
                                    if (minsRounded > 0) {
                                        message = "<b>" + minsRounded + "</b><small> Min.</small>";
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> Min.</small>";
                                    }
                                    if (!bus.optString("rmk_en").isEmpty()) {
                                        message += (message.isEmpty() ? bus.optString("rmk_en") : "<small> (" + bus.optString("rmk_en") + ")</small>");
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                                    } else if (minsRounded > -60) {
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

                                if (message.isEmpty()) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight(), typhoonInfo.getTyphoonWarningTitle());
                                    } else {
                                        message = "<b></b>-";
                                    }
                                } else {
                                    message = "<b></b>" + message;
                                }
                                lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                            }
                        }
                    }
                }
            } else if (co == Operator.NLB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=" + route.getNlbId() + "&stopId=" + stopId + "&language=" + Shared.Companion.getLanguage());
                if (data != null && data.length() > 0 && data.has("estimatedArrivals")) {
                    JSONArray buses = data.optJSONArray("estimatedArrivals");

                    for (int u = 0; u < buses.length(); u++) {
                        JSONObject bus = buses.optJSONObject(u);
                        int seq = u + 1;
                        String eta = bus.optString("estimatedArrivalTime") + "+08:00";
                        String variant = bus.optString("routeVariantName").trim();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
                        double mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                        long minsRounded = Math.round(mins);
                        String message = "";
                        if (language.equals("en")) {
                            if (minsRounded > 0) {
                                message = "<b>" + minsRounded + "</b><small> Min.</small>";
                            } else if (minsRounded > -60) {
                                message = "<b>-</b><small> Min.</small>";
                            }
                        } else {
                            if (minsRounded > 0) {
                                message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                            } else if (minsRounded > -60) {
                                message = "<b>-</b><small> 分鐘</small>";
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
                        lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                    }
                }
            } else if (co == Operator.MTR_BUS) {
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

                        double mins = eta / 60.0;
                        long minsRounded = (long) Math.floor(mins);

                        if (DATA.getMtrBusStopAlias().get(stopId).contains(busStopId)) {
                            String message = "";
                            if (language.equals("en")) {
                                if (minsRounded > 0) {
                                    message = "<b>" + minsRounded + "</b><small> Min.</small>";
                                } else if (minsRounded > -60) {
                                    message = "<b>-</b><small> Min.</small>";
                                }
                            } else {
                                if (minsRounded > 0) {
                                    message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                                } else if (minsRounded > -60) {
                                    message = "<b>-</b><small> 分鐘</small>";
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
                            lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                        }
                    }
                }
            } else if (co == Operator.GMB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight();

                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etagmb.gov.hk/eta/stop/" + stopId);
                Set<Integer> stopSequences = new HashSet<>();
                List<Triple<Integer, Double, JSONObject>> busList = new ArrayList<>();
                for (int i = 0; i < data.optJSONArray("data").length(); i++) {
                    JSONObject routeData = data.optJSONArray("data").optJSONObject(i);
                    JSONArray buses = routeData.optJSONArray("eta");
                    Optional<Route> filteredEntry = DATA.getDataSheet().getRouteList().values().stream()
                            .filter(r -> r.getBound().containsKey(Operator.GMB) && r.getGtfsId().equals(routeData.optString("route_id")))
                            .findFirst();
                    if (filteredEntry.isPresent() && buses != null) {
                        String routeNumber = filteredEntry.get().getRouteNumber();
                        int stopSeq = routeData.optInt("stop_seq");
                        for (int u = 0; u < buses.length(); u++) {
                            JSONObject bus = buses.optJSONObject(u);
                            if (routeNumber.equals(route.getRouteNumber())) {
                                String eta = bus.optString("timestamp");
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                double mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : (formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0;
                                stopSequences.add(stopSeq);
                                busList.add(new Triple<>(stopSeq, mins, bus));
                            }
                        }
                    }
                }
                if (stopSequences.size() > 1) {
                    int matchingSeq = stopSequences.stream().min(Comparator.comparing(i -> Math.abs(i - stopIndex))).orElse(-1);
                    busList.removeIf(t -> t.getFirst() != matchingSeq);
                }
                busList.sort(Comparator.comparing(Triple<Integer, Double, JSONObject>::getSecond));
                for (int i = 0; i < busList.size(); i++) {
                    Triple<Integer, Double, JSONObject> entry = busList.get(i);
                    JSONObject bus = entry.getThird();
                    int seq = i + 1;
                    String remark = language.equals("en") ? bus.optString("remarks_en") : bus.optString("remarks_tc");
                    if (remark == null || remark.equalsIgnoreCase("null")) {
                        remark = "";
                    }
                    double mins = entry.getSecond();
                    long minsRounded = Math.round(mins);
                    String message = "";
                    if (language.equals("en")) {
                        if (minsRounded > 0) {
                            message = "<b>" + minsRounded + "</b><small> Min.</small>";
                        } else if (minsRounded > -60) {
                            message = "<b>-</b><small> Min.</small>";
                        }
                    } else {
                        if (minsRounded > 0) {
                            message = "<b>" + minsRounded + "</b><small> 分鐘</small>";
                        } else if (minsRounded > -60) {
                            message = "<b>-</b><small> 分鐘</small>";
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
                    lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded));
                }
            } else if (co == Operator.LRT) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine();

                List<String> stopsList = route.getStops().get(Operator.LRT);
                if (stopsList.indexOf(stopId) + 1 >= stopsList.size()) {
                    isMtrEndOfLine = true;
                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "End of Line" : "終點站"));
                } else {
                    ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
                    int hour = hongKongTime.getHour();

                    List<LrtETAData> results = new ArrayList<>();
                    JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=" + stopId.substring(2));
                    if (data.optInt("status") != 0) {
                        JSONArray platformList = data.optJSONArray("platform_list");
                        for (int i = 0; i < platformList.length(); i++) {
                            JSONObject platform = platformList.optJSONObject(i);
                            int platformNumber = platform.optInt("platform_id");
                            JSONArray routeList = platform.optJSONArray("route_list");
                            if (routeList != null) {
                                for (int u = 0; u < routeList.length(); u++) {
                                    JSONObject routeData = routeList.optJSONObject(u);
                                    String routeNumber = routeData.optString("route_no");
                                    String destCh = routeData.optString("dest_ch");
                                    if (routeNumber.equals(route.getRouteNumber()) && isLrtStopOnOrAfter(stopId, destCh, route)) {
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
                    }
                    if (results.isEmpty()) {
                        if (hour < 3) {
                            lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出"));
                        } else if (hour < 6) {
                            lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service has not yet started" : "今日服務尚未開始"));
                        } else {
                            lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊"));
                        }
                    } else {
                        String lineColor = RouteExtensionsKt.getColorHex(co, route.getRouteNumber(), 0xFFFFFFFFL);
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
                            StringBuilder cartsMessage = new StringBuilder(lrt.getTrainLength() * 21);
                            for (int u = 0; u < lrt.getTrainLength(); u++) {
                                cartsMessage.append("<img src=\"lrv\">");
                            }
                            if (lrt.getTrainLength() == 1) {
                                cartsMessage.append("<img src=\"lrv_empty\">");
                            }
                            long mins = lrt.getEta();
                            String message = "<b></b><span style=\"color: " + lineColor + "\">" + StringUtils.getCircledNumber(lrt.getPlatformNumber()) + "</span> " + cartsMessage + " " + minsMessage;
                            lines.put(seq, ETALineEntry.etaEntry(message, toShortText(mins, 1), mins, mins));
                        }
                    }
                }
            } else if (co == Operator.MTR) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine();

                String lineName = route.getRouteNumber();
                String lineColor = RouteExtensionsKt.getColorHex(co, lineName, 0xFFFFFFFFL);

                String bound = route.getBound().get(Operator.MTR);
                if (isMtrStopEndOfLine(stopId, lineName, bound)) {
                    isMtrEndOfLine = true;
                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "End of Line" : "終點站"));
                } else {
                    ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
                    int hour = hongKongTime.getHour();
                    DayOfWeek dayOfWeek = hongKongTime.getDayOfWeek();

                    JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + lineName + "&sta=" + stopId);
                    if (data.optInt("status") == 0) {
                        lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊"));
                    } else {
                        JSONObject lineStops = data.optJSONObject("data").optJSONObject(lineName + "-" + stopId);
                        boolean raceDay = dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.SUNDAY;
                        if (lineStops == null) {
                            if (stopId.equals("RAC")) {
                                if (!raceDay) {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service on race days only" : "僅在賽馬日提供服務"));
                                } else if (hour >= 15 || hour < 3) {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出"));
                                } else {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service has not yet started" : "今日服務尚未開始"));
                                }
                            } else if (hour < 3 || (stopId.equals("LMC") && hour >= 10)) {
                                lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出"));
                            } else if (hour < 6) {
                                lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service has not yet started" : "今日服務尚未開始"));
                            } else {
                                lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊"));
                            }
                        } else {
                            boolean delayed = !data.optString("isdelay", "N").equals("N");
                            String dir = bound.equals("UT") ? "UP" : "DOWN";
                            JSONArray trains = lineStops.optJSONArray(dir);
                            if (trains == null || trains.length() == 0) {
                                if (stopId.equals("RAC")) {
                                    if (!raceDay) {
                                        lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service on race days only" : "僅在賽馬日提供服務"));
                                    } else if (hour >= 15 || hour < 3) {
                                        lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出"));
                                    } else {
                                        lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service has not yet started" : "今日服務尚未開始"));
                                    }
                                } else if (hour < 3 || (stopId.equals("LMC") && hour >= 10)) {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Last train has departed" : "尾班車已開出"));
                                } else if (hour < 6) {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Service has not yet started" : "今日服務尚未開始"));
                                } else {
                                    lines.put(1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Server unable to provide data" : "系統未能提供資訊"));
                                }
                            } else {
                                for (int u = 0; u < trains.length(); u++) {
                                    JSONObject trainData = trains.optJSONObject(u);
                                    int seq = Integer.parseInt(trainData.optString("seq"));
                                    int platform = Integer.parseInt(trainData.optString("plat"));
                                    String specialRoute = trainData.optString("route");
                                    String dest = DATA.getDataSheet().getStopList().get(trainData.optString("dest")).getName().get(Shared.Companion.getLanguage());
                                    if (!stopId.equals("AIR")) {
                                        if (dest.equals("博覽館")) {
                                            dest = "機場及博覽館";
                                        } else if (dest.equals("AsiaWorld-Expo")) {
                                            dest = "Airport & AsiaWorld-Expo";
                                        }
                                    }
                                    if (!specialRoute.isEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                        String via = DATA.getDataSheet().getStopList().get(specialRoute).getName().get(Shared.Companion.getLanguage());
                                        dest += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " via " : " 經") + via + "</small>";
                                    }
                                    String timeType = trainData.optString("timeType");
                                    String eta = trainData.optString("time");

                                    @SuppressLint("SimpleDateFormat")
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    format.setTimeZone(TimeZone.getTimeZone(hongKongTime.getZone()));
                                    double mins = (format.parse(eta).getTime() - Instant.now().toEpochMilli()) / 60000.0;
                                    long minsRounded = Math.round(mins);

                                    String minsMessage;
                                    if (minsRounded > 59) {
                                        minsMessage = "<b>" + hongKongTime.plusMinutes(minsRounded).format(DateTimeFormatter.ofPattern("HH:mm")) + "</b>";
                                    } else if (minsRounded > 1) {
                                        minsMessage = "<b>" + minsRounded + "</b><small>" + (Shared.Companion.getLanguage().equals("en") ? " Min." : " 分鐘") + "</small>";
                                    } else if (minsRounded == 1 && !timeType.equals("D")) {
                                        minsMessage = "<b>" + (Shared.Companion.getLanguage().equals("en") ? "Arriving" : "即將抵達") + "</b>";
                                    } else {
                                        minsMessage = "<b>" + (Shared.Companion.getLanguage().equals("en") ? "Departing" : "正在離開") + "</b>";
                                    }

                                    String message = "<b></b><span style=\"color: " + lineColor + "\">" + StringUtils.getCircledNumber(platform) + "</span> " + dest + " " + minsMessage;
                                    if (seq == 1) {
                                        if (delayed) {
                                            message += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " (Delayed)" : " (服務延誤)") + "</small>";
                                        }
                                    }
                                    lines.put(seq, ETALineEntry.etaEntry(message, toShortText(minsRounded, 1), mins, minsRounded));
                                }
                            }
                        }
                    }
                }
            }
            return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, nextCo, lines);
        });
        ETA_QUERY_EXECUTOR.submit(pending);
        return pending;
    }

    private ETAShortText toShortText(long minsRounded, long arrivingThreshold) {
        return new ETAShortText(minsRounded <= arrivingThreshold ? "-" : String.valueOf(minsRounded), Shared.Companion.getLanguage().equals("en") ? "Min." : "分鐘");
    }

    @Immutable
    public static class JointOperatedEntry implements Comparable<JointOperatedEntry> {

        private final double mins;
        private final long minsRounded;
        private final String line;
        private final Operator co;

        public JointOperatedEntry(double mins, long minsRounded, String line, Operator co) {
            this.mins = mins;
            this.minsRounded = minsRounded;
            this.line = line;
            this.co = co;
        }

        public double getMins() {
            return mins;
        }

        public long getMinsRounded() {
            return minsRounded;
        }

        public String getLine() {
            return line;
        }

        public Operator getCo() {
            return co;
        }

        @Override
        public int compareTo(JointOperatedEntry o) {
            return Double.compare(mins, o.mins);
        }

    }

    @Immutable
    public static class PendingETAQueryResult extends FutureTask<ETAQueryResult> {

        private final Context context;
        private final Operator co;

        public PendingETAQueryResult(Context context, Operator co, Callable<ETAQueryResult> callable) {
            super(callable);
            this.context = context;
            this.co = co;
        }

        private ETAQueryResult getErrorResult() {
            ConnectionUtils.BackgroundRestrictionType restrictionType = context instanceof ComponentActivity ? ConnectionUtils.BackgroundRestrictionType.NONE : ConnectionUtils.isBackgroundRestricted(context);
            return ETAQueryResult.connectionError(restrictionType, co);
        }

        @Override
        public ETAQueryResult get() {
            try {
                return super.get();
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                e.printStackTrace();
                try { cancel(true); } catch (Throwable ignore) {}
                return getErrorResult();
            }
        }

        @Override
        public ETAQueryResult get(long timeout, TimeUnit unit) {
            try {
                return super.get(timeout, unit);
            } catch (ExecutionException | InterruptedException | TimeoutException | CancellationException e) {
                e.printStackTrace();
                try { cancel(true); } catch (Throwable ignore) {}
                return getErrorResult();
            }
        }

    }

    @Immutable
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

    @Immutable
    public static class ETALineEntry {

        public static final ETALineEntry EMPTY = new ETALineEntry("-", ETAShortText.EMPTY, -1, -1);

        public static ETALineEntry textEntry(String text) {
            return new ETALineEntry(text, ETAShortText.EMPTY, -1, -1);
        }

        public static ETALineEntry etaEntry(String text, ETAShortText shortText, double eta, long etaRounded) {
            if (etaRounded > -60) {
                etaRounded = Math.max(0, etaRounded);
                eta = Math.max(0, eta);
            } else {
                etaRounded = -1;
                eta = -1;
            }
            return new ETALineEntry(text, shortText, eta, etaRounded);
        }

        private final String text;
        private final ETAShortText shortText;
        private final double eta;
        private final long etaRounded;

        private ETALineEntry(String text, ETAShortText shortText, double eta, long etaRounded) {
            this.text = text;
            this.shortText = shortText;
            this.eta = eta;
            this.etaRounded = etaRounded;
        }

        public String getText() {
            return text;
        }

        public ETAShortText getShortText() {
            return shortText;
        }

        public double getEta() {
            return eta;
        }

        public long getEtaRounded() {
            return etaRounded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ETALineEntry that = (ETALineEntry) o;
            return Double.compare(that.eta, eta) == 0 && etaRounded == that.etaRounded && Objects.equals(text, that.text) && Objects.equals(shortText, that.shortText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, shortText, eta, etaRounded);
        }
    }

    @Immutable
    public static class ETAShortText {

        public static final ETAShortText EMPTY = new ETAShortText("", "");

        private final String first;
        private final String second;

        public ETAShortText(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public String getSecond() {
            return second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ETAShortText that = (ETAShortText) o;
            return Objects.equals(first, that.first) && Objects.equals(second, that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }

    @Immutable
    public static class ETAQueryResult {

        public static ETAQueryResult connectionError(ConnectionUtils.BackgroundRestrictionType restrictionType, Operator co) {
            Map<Integer, ETALineEntry> lines;
            switch (restrictionType) {
                case POWER_SAVE_MODE: {
                    lines = Map.of(
                            1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Background Internet Restricted" : "背景網絡存取被限制"),
                            2, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Power Saving" : "省電模式")
                    );
                    break;
                }
                case RESTRICT_BACKGROUND_STATUS: {
                    lines = Map.of(
                            1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Background Internet Restricted" : "背景網絡存取被限制"),
                            2, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Data Saver" : "數據節省器")
                    );
                    break;
                }
                case LOW_POWER_STANDBY: {
                    lines = Map.of(
                            1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Background Internet Restricted" : "背景網絡存取被限制"),
                            2, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Low Power Standby" : "低耗電待機")
                    );
                    break;
                }
                default: {
                    lines = Map.of(
                            1, ETALineEntry.textEntry(Shared.Companion.getLanguage().equals("en") ? "Unable to Connect" : "無法連接伺服器")
                    );
                    break;
                }
            }
            return new ETAQueryResult(true, false, false, co, lines);
        }

        public static ETAQueryResult result(boolean isMtrEndOfLine, boolean isTyphoonSchedule, Operator nextCo, Map<Integer, ETALineEntry> lines) {
            return new ETAQueryResult(false, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines);
        }

        private final boolean isConnectionError;
        private final boolean isMtrEndOfLine;
        private final boolean isTyphoonSchedule;
        private final Operator nextCo;
        private final Map<Integer, ETALineEntry> lines;
        private final long nextScheduledBus;

        private ETAQueryResult(boolean isConnectionError, boolean isMtrEndOfLine, boolean isTyphoonSchedule, Operator nextCo, Map<Integer, ETALineEntry> lines) {
            this.isConnectionError = isConnectionError;
            this.isMtrEndOfLine = isMtrEndOfLine;
            this.isTyphoonSchedule = isTyphoonSchedule;
            this.nextCo = nextCo;
            this.lines = Collections.unmodifiableMap(lines);

            ETALineEntry entry = lines.get(1);
            this.nextScheduledBus = entry == null ? -1 : entry.getEtaRounded();
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

        public Operator getNextCo() {
            return nextCo;
        }

        public Map<Integer, ETALineEntry> getRawLines() {
            return lines;
        }

        public ETALineEntry getLine(int index) {
            return lines.getOrDefault(index, ETALineEntry.EMPTY);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ETAQueryResult result = (ETAQueryResult) o;
            return isConnectionError == result.isConnectionError && isMtrEndOfLine == result.isMtrEndOfLine && isTyphoonSchedule == result.isTyphoonSchedule && nextScheduledBus == result.nextScheduledBus && Objects.equals(nextCo, result.nextCo) && Objects.equals(lines, result.lines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isConnectionError, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines, nextScheduledBus);
        }
    }

    public enum State {

        LOADING(true), UPDATE_CHECKING(true), UPDATING(true), READY(false), ERROR(false);

        private final boolean isProcessing;

        State(boolean isProcessing) {
            this.isProcessing = isProcessing;
        }

        public boolean isProcessing() {
            return isProcessing;
        }
    }

}
