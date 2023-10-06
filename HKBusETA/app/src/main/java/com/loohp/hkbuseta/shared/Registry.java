package com.loohp.hkbuseta.shared;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.compose.runtime.Stable;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileUpdateRequester;

import com.loohp.hkbuseta.MainActivity;
import com.loohp.hkbuseta.branchedlist.BranchedList;
import com.loohp.hkbuseta.tiles.EtaTileServiceEight;
import com.loohp.hkbuseta.tiles.EtaTileServiceFive;
import com.loohp.hkbuseta.tiles.EtaTileServiceFour;
import com.loohp.hkbuseta.tiles.EtaTileServiceOne;
import com.loohp.hkbuseta.tiles.EtaTileServiceSeven;
import com.loohp.hkbuseta.tiles.EtaTileServiceSix;
import com.loohp.hkbuseta.tiles.EtaTileServiceThree;
import com.loohp.hkbuseta.tiles.EtaTileServiceTwo;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Registry {

    private static Registry INSTANCE = null;

    public static synchronized Registry getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Registry(context);
        }
        return INSTANCE;
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

    private static JSONObject PREFERENCES = null;
    private static JSONObject DATA_SHEET = null;
    private static Set<String> BUS_ROUTE = null;
    private static JSONObject MTR_BUS_STOP_ALIAS = null;

    private volatile State state = State.LOADING;
    private volatile float updatePercentage = 0F;
    private volatile boolean preferencesLoaded = false;

    private boolean isAboveTyphoonSignalEight = false;
    private String typhoonWarningTitle = "";
    private String currentTyphoonSignalId = "";

    private Registry(Context context) {
        try {
            ensureData(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=" + (Shared.Companion.getLanguage().equals("en") ? "en" : "tc"));
                if (data != null && data.has("WTCSGNL")) {
                    Matcher matcher = Pattern.compile("TC([0-9]+)(.*)").matcher(data.optJSONObject("WTCSGNL").optString("code"));
                    if (matcher.find() && matcher.group(1) != null) {
                        int signal = Integer.parseInt(matcher.group(1));
                        isAboveTyphoonSignalEight = signal >= 8;
                        if (Shared.Companion.getLanguage().equals("en")) {
                            typhoonWarningTitle = data.optJSONObject("WTCSGNL").optString("type") + " is in force";
                        } else {
                            typhoonWarningTitle = data.optJSONObject("WTCSGNL").optString("type") + " 現正生效";
                        }
                        if (signal < 8) {
                            currentTyphoonSignalId = "tc" + signal + (matcher.group(2) != null ? matcher.group(2) : "").toLowerCase();
                        } else {
                            currentTyphoonSignalId = "tc" + StringUtils.padStart(String.valueOf(signal), 2, '0') + (matcher.group(2) != null ? matcher.group(2) : "").toLowerCase();
                        }
                        return;
                    }
                }
                isAboveTyphoonSignalEight = false;
                typhoonWarningTitle = "";
                currentTyphoonSignalId = "";
            }
        }, 0, Shared.ETA_UPDATE_INTERVAL);
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
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("language", language);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(MainActivity.Companion.getContext().getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasFavouriteRouteStop(int favoriteIndex) {
        return Shared.Companion.getFavoriteRouteStops().get(favoriteIndex) != null;
    }

    public boolean isFavouriteRouteStop(int favoriteIndex, String stopId, String co, int index, JSONObject stop, JSONObject route) {
        JSONObject json = Shared.Companion.getFavoriteRouteStops().get(favoriteIndex);
        if (json == null) {
            return false;
        }
        if (!stopId.equals(json.optString("stopId"))) {
            return false;
        }
        if (!co.equals(json.optString("co"))) {
            return false;
        }
        if (index != json.optInt("index")) {
            return false;
        }
        if (!stop.optJSONObject("name").optString("zh").equals(json.optJSONObject("stop").optJSONObject("name").optString("zh"))) {
            return false;
        }
        return route.optString("route").equals(json.optJSONObject("route").optString("route"));
    }

    public void clearFavouriteRouteStop(int favoriteIndex, Context context) {
        try {
            Shared.Companion.getFavoriteRouteStops().remove(favoriteIndex);
            if (PREFERENCES != null && PREFERENCES.has("favouriteRouteStops")) {
                PREFERENCES.optJSONObject("favouriteRouteStops").remove(String.valueOf(favoriteIndex));
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(MainActivity.Companion.getContext().getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(PREFERENCES.toString());
                    pw.flush();
                }
            }
            updateTileService(favoriteIndex, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFavouriteRouteStop(int favoriteIndex, String stopId, String co, int index, JSONObject stop, JSONObject route, Context context) {
        try {
            JSONObject json = new JSONObject();
            json.put("stopId", stopId);
            json.put("co", co);
            json.put("index", index);
            json.put("stop", stop);
            json.put("route", route);
            Shared.Companion.getFavoriteRouteStops().put(favoriteIndex, json);
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            if (!PREFERENCES.has("favouriteRouteStops")) {
                PREFERENCES.put("favouriteRouteStops", new JSONObject());
            }
            PREFERENCES.optJSONObject("favouriteRouteStops").put(String.valueOf(favoriteIndex), json);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(MainActivity.Companion.getContext().getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(favoriteIndex, context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAboveTyphoonSignalEight() {
        return isAboveTyphoonSignalEight;
    }

    public String getTyphoonWarningTitle() {
        return typhoonWarningTitle;
    }

    public String getCurrentTyphoonSignalId() {
        return currentTyphoonSignalId;
    }

    private void ensureData(Context context) throws IOException {
        if (state.equals(State.READY)) {
            return;
        }
        if (PREFERENCES != null && DATA_SHEET != null && BUS_ROUTE != null && MTR_BUS_STOP_ALIAS != null) {
            return;
        }

        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8))) {
                PREFERENCES = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                Shared.Companion.setLanguage(PREFERENCES.optString("language", "zh"));
                if (PREFERENCES.has("favouriteRouteStops")) {
                    JSONObject favoriteRouteStops = PREFERENCES.optJSONObject("favouriteRouteStops");
                    for (Iterator<String> itr = favoriteRouteStops.keys(); itr.hasNext(); ) {
                        String key = itr.next();
                        Shared.Companion.getFavoriteRouteStops().put(Integer.parseInt(key), favoriteRouteStops.optJSONObject(key));
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                PREFERENCES = new JSONObject();
                PREFERENCES.put("language", "zh");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        preferencesLoaded = true;

        Thread thread = new Thread(() -> {
            try {
                ConnectionUtils.ConnectionType connectionType = ConnectionUtils.getConnectionType(context);

                Supplier<String> checksumFetcher = () -> {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    ForkJoinPool.commonPool().execute(() -> {
                        future.complete(HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/checksum.md5"));
                    });
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
                        DATA_SHEET = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(BUS_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        BUS_ROUTE = JsonUtils.toSet(new JSONArray(reader.lines().collect(Collectors.joining("\n"))), String.class);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(MTR_BUS_STOP_ALIAS_FILE_NAME), StandardCharsets.UTF_8))) {
                        MTR_BUS_STOP_ALIAS = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    state = State.READY;
                } else if (!connectionType.hasConnection()) {
                    state = State.ERROR;
                } else {
                    state = State.UPDATING;
                    updatePercentage = 0F;

                    JSONObject data = HTTPRequestUtils.getJSONResponseWithPercentageCallback("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/data.json", p -> updatePercentage = p * 0.85F);
                    updatePercentage = 0.95F;
                    MTR_BUS_STOP_ALIAS = data.optJSONObject("mtrBusStopAlias");
                    DATA_SHEET = data.optJSONObject("dataSheet");
                    BUS_ROUTE = JsonUtils.toSet(data.optJSONArray("busRoute"), String.class);
                    updatePercentage = 1F;

                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(MTR_BUS_STOP_ALIAS_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(MTR_BUS_STOP_ALIAS.toString());
                        pw.flush();
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(DATA_SHEET_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(DATA_SHEET.toString());
                        pw.flush();
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(BUS_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(JsonUtils.fromCollection(BUS_ROUTE).toString());
                        pw.flush();
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(CHECKSUM_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(checksum == null ? "" : checksum);
                        pw.flush();
                    }
                }
                updatePercentage = 1F;
                if (!state.equals(State.ERROR)) {
                    state = State.READY;
                    updateTileService(context);
                }
            } catch (Exception e) {
                Log.e("Resource Downloading Exception", "Exception: ", e);
                state = State.ERROR;
            }
        });
        thread.start();
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

    public List<JSONObject> findRoutes(String input, boolean exact) {
        return findRoutes(input, exact, r -> true);
    }

    public List<JSONObject> findRoutes(String input, boolean exact, Predicate<JSONObject> predicate) {
        Predicate<String> routeMatcher = exact ? r -> r.equals(input) : r -> r.startsWith(input);
        try {
            Map<String, JSONObject> matchingRoutes = new HashMap<>();

            for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                String key = itr.next();
                JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                if (data.optBoolean("ctbIsCircular")) {
                    continue;
                }
                if (routeMatcher.test(data.optString("route")) && predicate.test(data)) {
                    String co;
                    JSONObject bound = data.optJSONObject("bound");
                    if (bound.has("kmb")) {
                        co = "kmb";
                    } else if (bound.has("ctb")) {
                        co = "ctb";
                    } else if (bound.has("nlb")) {
                        co = "nlb";
                    } else if (bound.has("mtr-bus")) {
                        co = "mtr-bus";
                    } else if (bound.has("gmb")) {
                        co = "gmb";
                    } else if (bound.has("lightRail")) {
                        co = "lightRail";
                    } else if (bound.has("mtr")) {
                        co = "mtr";
                    } else {
                        continue;
                    }
                    String key0 = data.optString("route") + "," + co + "," + (co.equals("nlb") ? data.optString("nlbId") : data.optJSONObject("bound").optString(co));

                    if (matchingRoutes.containsKey(key0)) {
                        try {
                            JSONObject existingMatchingRoute = matchingRoutes.get(key0);

                            int type = Integer.parseInt(data.optString("serviceType"));
                            int matchingType = Integer.parseInt(existingMatchingRoute.optJSONObject("route").optString("serviceType"));

                            if (type < matchingType) {
                                existingMatchingRoute.put("route", data);
                                existingMatchingRoute.put("co", co);
                            }
                        } catch (NumberFormatException ignore) {}
                    } else {
                        JSONObject newMatchingRoute = new JSONObject();
                        newMatchingRoute.put("route", data);
                        newMatchingRoute.put("co", co);
                        matchingRoutes.put(key0, newMatchingRoute);
                    }
                }
            }

            if (matchingRoutes.isEmpty()) {
                return null;
            }

            List<JSONObject> routes = new ArrayList<>(matchingRoutes.values());
            routes.sort((a, b) -> {
                JSONObject boundA = a.optJSONObject("route").optJSONObject("bound");
                JSONObject boundB = b.optJSONObject("route").optJSONObject("bound");
                String coAStr = boundA.has("kmb") ? "kmb" : (boundA.has("ctb") ? "ctb" : (boundA.has("nlb") ? "nlb" : (boundA.has("mtr-bus") ? "mtr-bus" : (boundA.has("gmb") ? "gmb" : (boundA.has("lightRail") ? "lightRail" : "mtr")))));
                String coBStr = boundB.has("kmb") ? "kmb" : (boundB.has("ctb") ? "ctb" : (boundB.has("nlb") ? "nlb" : (boundB.has("mtr-bus") ? "mtr-bus" : (boundB.has("gmb") ? "gmb" : (boundB.has("lightRail") ? "lightRail" : "mtr")))));
                int coA = coAStr.equals("kmb") ? 0 : (coAStr.equals("ctb") ? 1 : (coAStr.equals("nlb") ? 2 : (coAStr.equals("mtr-bus") ? 3 : (coAStr.equals("gmb") ? 4 : (coAStr.equals("lightRail") ? 5 : 6)))));
                int coB = coBStr.equals("kmb") ? 0 : (coBStr.equals("ctb") ? 1 : (coBStr.equals("nlb") ? 2 : (coBStr.equals("mtr-bus") ? 3 : (coBStr.equals("gmb") ? 4 : (coBStr.equals("lightRail") ? 5 : 6)))));
                if (coA != coB) {
                    return coA - coB;
                }

                JSONObject routeA = a.optJSONObject("route");
                JSONObject routeB = b.optJSONObject("route");

                String routeNumberA = routeA.optString("route");
                String routeNumberB = routeB.optString("route");

                if (coA == 5 || coA == 6) {
                    int lineDiff = routeNumberA.compareTo(routeNumberB);
                    if (lineDiff != 0) {
                        return lineDiff;
                    }
                    return -boundA.optString("mtr").compareTo(boundB.optString("mtr"));
                }
                if (coA == 2) {
                    return IntUtils.parseOrZero(routeA.optString("nlbId")) - IntUtils.parseOrZero(routeB.optString("nlbId"));
                }
                if (coA == 4) {
                    int gtfsDiff = IntUtils.parseOrZero(routeA.optString("gtfsId")) - IntUtils.parseOrZero(routeB.optString("gtfsId"));
                    if (gtfsDiff != 0) {
                        return gtfsDiff;
                    }
                }
                int typeDiff = IntUtils.parseOrZero(routeA.optString("serviceType")) - IntUtils.parseOrZero(routeB.optString("serviceType"));
                if (typeDiff == 0) {
                    if (coA == 1) {
                        return Boolean.compare(routeA.has("ctbSpecial"), routeB.has("ctbSpecial"));
                    }
                    return -boundA.optString(coAStr).compareTo(boundB.optString(coBStr));
                }
                return typeDiff;
            });
            return routes;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public NearbyRoutesResult getNearbyRoutes(double lat, double lng, Set<String> excludedRouteNumbers, boolean isInterchangeSearch) {
        try {
            JSONObject origin = new JSONObject().put("lat", lat).put("lng", lng);

            JSONObject stops = DATA_SHEET.optJSONObject("stopList");
            List<JSONObject> nearbyStops = new ArrayList<>();

            JSONObject closestStop = null;
            double closestDistance = Double.MAX_VALUE;

            for (Iterator<String> itr = stops.keys(); itr.hasNext(); ) {
                String stopId = itr.next();
                JSONObject entry = stops.optJSONObject(stopId);
                JSONObject location = entry.optJSONObject("location");
                double distance = DistanceUtils.findDistance(lat, lng, location.optDouble("lat"), location.optDouble("lng"));

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

                    JSONObject nearbyStop = new JSONObject();
                    nearbyStop.put("stopId", stopId);
                    nearbyStop.put("data", entry);
                    nearbyStop.put("distance", distance);
                    nearbyStop.put("co", co);
                    nearbyStops.add(nearbyStop);
                }
            }

            Map<String, JSONObject> nearbyRoutes = new HashMap<>();

            for (JSONObject nearbyStop : nearbyStops) {
                String stopId = nearbyStop.optString("stopId");

                for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                    String key = itr.next();
                    JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);

                    if (excludedRouteNumbers.contains(data.optString("route"))) {
                        continue;
                    }
                    if (data.optBoolean("ctbIsCircular")) {
                        continue;
                    }

                    boolean isKmb = data.has("bound") && data.optJSONObject("bound").has("kmb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("kmb"), stopId);
                    boolean isCtb = data.has("bound") && data.optJSONObject("bound").has("ctb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("ctb"), stopId);
                    boolean isNlb = data.has("bound") && data.optJSONObject("bound").has("nlb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("nlb"), stopId);
                    boolean isMtrBus = data.has("bound") && data.optJSONObject("bound").has("mtr-bus") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("mtr-bus"), stopId);
                    boolean isGmb = data.has("bound") && data.optJSONObject("bound").has("gmb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("gmb"), stopId);
                    boolean isLrt = data.has("bound") && data.optJSONObject("bound").has("lightRail") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("lightRail"), stopId);
                    boolean isMtr = data.has("bound") && data.optJSONObject("bound").has("mtr") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("mtr"), stopId);

                    if (isKmb || isCtb || isNlb || isMtrBus || isGmb || isLrt || isMtr) {
                        String co = isKmb ? "kmb" : (isCtb ? "ctb" : (isNlb ? "nlb" : (isMtrBus ? "mtr-bus" : (isGmb ? "gmb" : (isLrt ? "lightRail" : "mtr")))));
                        String key0 = data.optString("route") + "," + co + "," + (co.equals("nlb") ? data.optString("nlbId") : data.optJSONObject("bound").optString(co));

                        if (nearbyRoutes.containsKey(key0)) {
                            JSONObject existingNearbyRoute = nearbyRoutes.get(key0);

                            if (existingNearbyRoute.optJSONObject("stop").optDouble("distance") > nearbyStop.optDouble("distance")) {
                                try {
                                    int type = Integer.parseInt(data.optString("serviceType"));
                                    int matchingType = Integer.parseInt(existingNearbyRoute.optJSONObject("route").optString("serviceType"));

                                    if (type < matchingType) {
                                        existingNearbyRoute.put("routeKey", key);
                                        existingNearbyRoute.put("stop", nearbyStop);
                                        existingNearbyRoute.put("route", data);
                                        existingNearbyRoute.put("co", co);
                                        existingNearbyRoute.put("origin", origin);
                                        existingNearbyRoute.put("interchangeSearch", isInterchangeSearch);
                                    }
                                } catch (NumberFormatException ignore) {
                                    existingNearbyRoute.put("routeKey", key);
                                    existingNearbyRoute.put("stop", nearbyStop);
                                    existingNearbyRoute.put("route", data);
                                    existingNearbyRoute.put("co", co);
                                    existingNearbyRoute.put("origin", origin);
                                    existingNearbyRoute.put("interchangeSearch", isInterchangeSearch);
                                }
                            }
                        } else {
                            JSONObject newNearbyRoute = new JSONObject();
                            newNearbyRoute.put("routeKey", key);
                            newNearbyRoute.put("stop", nearbyStop);
                            newNearbyRoute.put("route", data);
                            newNearbyRoute.put("co", co);
                            newNearbyRoute.put("origin", origin);
                            newNearbyRoute.put("interchangeSearch", isInterchangeSearch);
                            nearbyRoutes.put(key0, newNearbyRoute);
                        }
                    }
                }
            }

            if (nearbyRoutes.isEmpty()) {
                return new NearbyRoutesResult(Collections.emptyList(), closestStop, closestDistance);
            }

            List<JSONObject> routes = new ArrayList<>(nearbyRoutes.values());
            ZonedDateTime hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"));
            int hour = hongKongTime.getHour();
            boolean isNight = hour >= 1 && hour < 5;
            DayOfWeek weekday = hongKongTime.getDayOfWeek();
            String date = hongKongTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            boolean isHoliday = weekday.equals(DayOfWeek.SATURDAY) || weekday.equals(DayOfWeek.SUNDAY) || JsonUtils.contains(DATA_SHEET.optJSONArray("holidays"), date);

            routes.sort(Comparator.comparing((JSONObject a) -> {
                JSONObject route = a.optJSONObject("route");
                String routeNumber = route.optString("route");
                JSONObject bound = route.optJSONObject("bound");

                String pa = String.valueOf(routeNumber.charAt(0));
                String sa = String.valueOf(routeNumber.charAt(routeNumber.length() - 1));
                int na = IntUtils.parseOrZero(routeNumber.replaceAll("[^0-9]", ""));

                if (bound.has("gmb")) {
                    na += 1000;
                } else if (bound.has("mtr")) {
                    na += 2000;
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
                if (!pa.matches("[0-9]") && !pa.equals("K")) {
                    na += 400;
                }
                return na;
            }).thenComparing(a -> {
                return a.optJSONObject("route").optString("route");
            }).thenComparing(a -> {
                return IntUtils.parseOrZero(a.optJSONObject("route").optString("serviceType"));
            }).thenComparing(a -> {
                String co = a.optString("co");
                return co.equals("kmb") ? 0 : (co.equals("ctb") ? 1 : (co.equals("nlb") ? 2 : (co.equals("mtr-bus") ? 3 : (co.equals("gmb") ? 4 : (co.equals("lightRail") ? 5 : 6)))));
            }).thenComparing(Comparator.comparing((JSONObject a) -> {
                JSONObject bound = a.optJSONObject("route").optJSONObject("bound");
                if (bound.has("mtr")) {
                    return bound.optString("mtr");
                }
                return "";
            }).reversed()));

            Set<String> addedKeys = new HashSet<>();
            List<JSONObject> distinctRoutes = new ArrayList<>();
            for (JSONObject value : routes) {
                if (addedKeys.add(value.optString("routeKey"))) {
                    distinctRoutes.add(value);
                }
            }

            return new NearbyRoutesResult(distinctRoutes, closestStop, closestDistance);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static class NearbyRoutesResult {

        private final List<JSONObject> result;
        private final JSONObject closestStop;
        private final double closestDistance;

        public NearbyRoutesResult(List<JSONObject> result, JSONObject closestStop, double closestDistance) {
            this.result = result;
            this.closestStop = closestStop;
            this.closestDistance = closestDistance;
        }

        public List<JSONObject> getResult() {
            return result;
        }

        public JSONObject getClosestStop() {
            return closestStop;
        }

        public double getClosestDistance() {
            return closestDistance;
        }
    }

    public List<StopData> getAllStops(String routeNumber, String bound, String co, String gtfsId) {
        try {
            List<Pair<BranchedList<String, StopData>, Integer>> lists = new ArrayList<>();
            for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                String key = itr.next();
                JSONObject route = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                if (routeNumber.equals(route.optString("route")) && JsonUtils.contains(route.optJSONArray("co"), co)) {
                    boolean flag;
                    if (co.equals("nlb")) {
                        flag = bound.equals(route.optString("nlbId"));
                    } else {
                        flag = bound.equals(route.optJSONObject("bound").optString(co));
                        if (co.equals("gmb")) {
                            flag &= gtfsId.equals(route.optString("gtfsId"));
                        }
                    }
                    if (flag) {
                        BranchedList<String, StopData> localStops = new BranchedList<>();
                        JSONArray stops = route.optJSONObject("stops").optJSONArray(co);
                        int serviceType = IntUtils.parseOr(route.optString("serviceType"), 1);
                        for (int i = 0; i < stops.length(); i++) {
                            String stopId = stops.optString(i);
                            localStops.add(stopId, new StopData(stopId, serviceType, DATA_SHEET.optJSONObject("stopList").optJSONObject(stopId)));
                        }
                        lists.add(Pair.create(localStops, serviceType));
                    }
                }
            }
            lists.sort(Comparator.comparing(p -> p.second));
            BranchedList<String, StopData> result = new BranchedList<>((a, b) -> a.getServiceType() > b.getServiceType() ? b : a);
            for (Pair<BranchedList<String, StopData>, Integer> pair : lists) {
                result.merge(pair.first);
            }
            return result.values();
        } catch (Throwable e) {
            throw new RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gtfsId + ": " + e.getMessage(), e);
        }
    }

    public static class StopData {

        private final String stopId;
        private final int serviceType;
        private final JSONObject stop;

        public StopData(String stopId, int serviceType, JSONObject stop) {
            this.stopId = stopId;
            this.serviceType = serviceType;
            this.stop = stop;
        }

        public String getStopId() {
            return stopId;
        }

        public int getServiceType() {
            return serviceType;
        }

        public JSONObject getStop() {
            return stop;
        }
    }

    public static boolean isMtrStopOnOrAfter(String stopId, String relativeTo, String lineName, String bound) {
        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
            String key = itr.next();
            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
            if (lineName.equals(data.optString("route")) && data.optJSONObject("bound").optString("mtr").endsWith(bound)) {
                JSONArray stopsList = data.optJSONObject("stops").optJSONArray("mtr");
                int index = JsonUtils.indexOf(stopsList, stopId);
                int indexRef = JsonUtils.indexOf(stopsList, relativeTo);
                if (indexRef >= 0 && index >= indexRef) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMtrStopEndOfLine(String stopId, String lineName, String bound) {
        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
            String key = itr.next();
            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
            if (lineName.equals(data.optString("route")) && data.optJSONObject("bound").optString("mtr").endsWith(bound)) {
                JSONArray stopsList = data.optJSONObject("stops").optJSONArray("mtr");
                int index = JsonUtils.indexOf(stopsList, stopId);
                if (index >= 0 && index + 1 < stopsList.length()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getNoScheduledDepartureMessage(String altMessage, boolean isAboveTyphoonSignalEight, String typhoonWarningTitle) {
        if (altMessage == null || altMessage.isEmpty()) {
            altMessage = Shared.Companion.getLanguage().equals("en") ? "No scheduled departures at this moment" : "暫時沒有預定班次";
        }
        if (isAboveTyphoonSignalEight) {
            altMessage += " (" + typhoonWarningTitle + ")";
        }
        if (isAboveTyphoonSignalEight) {
            return "<span style=\"color: #6472BC;\">" + altMessage + "</span>";
        } else {
            return altMessage;
        }
    }

    public static ETAQueryResult getEta(String stopId, String co, JSONObject route, Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return ETAQueryResult.connectionError(co);
        }
        CompletableFuture<ETAQueryResult> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Map<Integer, String> lines = new HashMap<>();
                long nextScheduledBus = -999;
                lines.put(1, getNoScheduledDepartureMessage(null, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle()));
                String language = Shared.Companion.getLanguage();
                String nextCo = co;
                if (route.optBoolean("kmbCtbJoint", false)) {
                    String dest = route.optJSONObject("dest").optString("zh").replace(" ", "");
                    String orig = route.optJSONObject("orig").optString("zh").replace(" ", "");
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
                                if (routeNumber.equals(route.optString("route")) && bound.equals(route.optJSONObject("bound").optString("kmb"))) {
                                    String eta = bus.optString("eta");
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                    if (!eta.isEmpty() && !eta.equalsIgnoreCase("null")) {
                                        long mins = Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b>" + " Min." + "";
                                            } else if (mins > -60) {
                                                message = "<b>-</b>" + " Min." + "";
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                            } else if (mins > -60) {
                                                message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : " (" + bus.optString("rmk_tc") + ")");
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
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                            }
                                        } else {
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : " (" + bus.optString("rmk_tc") + ")");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");
                                        if (message.isEmpty()) {
                                            message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                        String routeNumber = route.optString("route");
                        JSONArray matchingStops = DATA_SHEET.optJSONObject("stopMap").optJSONArray(stopId);
                        List<String> ctbStopIds = new ArrayList<>();
                        if (matchingStops != null) {
                            for (int k = 0; k < matchingStops.length(); k++) {
                                JSONArray stopArray = matchingStops.optJSONArray(k);
                                if ("ctb".equals(stopArray.optString(0))) {
                                    ctbStopIds.add(stopArray.optString(1));
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
                                                    message = "<b>" + mins + "</b>" + " Min." + "";
                                                } else if (mins > -60) {
                                                    message = "<b>-</b>" + " Min." + "";
                                                }
                                                if (!bus.optString("rmk_en").isEmpty()) {
                                                    message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                                }
                                            } else {
                                                if (mins > 0) {
                                                    message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                } else if (mins > -60) {
                                                    message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                }
                                                if (!bus.optString("rmk_tc").isEmpty()) {
                                                    message += (message.isEmpty() ? bus.optString("rmk_tc") : " (" + bus.optString("rmk_tc") + ")");
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
                            lines.put(1, getNoScheduledDepartureMessage(null, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle()));
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
                                message += Shared.Companion.getLanguage().equals("en") ? " (Scheduled Bus)" : " (預定班次)";
                            }
                            if (entryCo.equals("kmb")) {
                                if (Shared.Companion.isLWBRoute(route.optString("route"))) {
                                    message += Shared.Companion.getLanguage().equals("en") ? " - LWB" : " - 龍運";
                                } else {
                                    message += Shared.Companion.getLanguage().equals("en") ? " - KMB" : " - 九巴";
                                }
                            } else {
                                message += Shared.Companion.getLanguage().equals("en") ? " - CTB" : " - 城巴";
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
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/" + stopId);
                            JSONArray buses = data.optJSONArray("data");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if ("KMB".equals(bus.optString("co"))) {
                                    String routeNumber = bus.optString("route");
                                    String bound = bus.optString("dir");
                                    if (routeNumber.equals(route.optString("route")) && bound.equals(route.optJSONObject("bound").optString("kmb"))) {
                                        int seq = bus.optInt("eta_seq");
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : " (" + bus.optString("rmk_tc") + ")");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty() || (INSTANCE.isAboveTyphoonSignalEight() && (message.equals("ETA service suspended") || message.equals("暫停預報")))) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                            String routeNumber = route.optString("route");
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/" + stopId + "/" + routeNumber);
                            JSONArray buses = data.optJSONArray("data");

                            for (int u = 0; u < buses.length(); u++) {
                                JSONObject bus = buses.optJSONObject(u);
                                if ("CTB".equals(bus.optString("co"))) {
                                    String bound = bus.optString("dir");
                                    if (routeNumber.equals(bus.optString("route")) && bound.equals(route.optJSONObject("bound").optString("ctb"))) {
                                        int seq = bus.optInt("eta_seq");
                                        String eta = bus.optString("eta");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_en").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!bus.optString("rmk_tc").isEmpty()) {
                                                message += (message.isEmpty() ? bus.optString("rmk_tc") : " (" + bus.optString("rmk_tc") + ")");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty()) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=" + route.optString("nlbId") + "&stopId=" + stopId + "&language=" + Shared.Companion.getLanguage());
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
                                        message = "<b>" + mins + "</b>" + " Min." + "";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b>" + " Min." + "";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                    if (!variant.isEmpty()) {
                                        message += (message.isEmpty() ? variant : " (" + variant + ")");
                                    }
                                } else {
                                    if (mins > 0) {
                                        message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                    if (!variant.isEmpty()) {
                                        message += (message.isEmpty() ? variant : " (" + variant + ")");
                                    }
                                }
                                message = message
                                        .replaceAll("原定", "預定")
                                        .replaceAll("最後班次", "尾班車")
                                        .replaceAll("尾班車已過", "尾班車已過本站");

                                if (message.isEmpty()) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                            String routeNumber = route.optString("route");
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

                                    if (JsonUtils.contains(MTR_BUS_STOP_ALIAS.optJSONArray(stopId), busStopId)) {
                                        String message = "";
                                        if (language.equals("en")) {
                                            if (mins > 0) {
                                                message = "<b>" + mins + "</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<b>-</b>" + " Min." + "";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!remark.isEmpty()) {
                                                message += (message.isEmpty() ? remark : " (" + remark + ")");
                                            }
                                        } else {
                                            if (mins > 0) {
                                                message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            } else if (mins > -60) {
                                                message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                }
                                            }
                                            if (!remark.isEmpty()) {
                                                message += (message.isEmpty() ? remark : " (" + remark + ")");
                                            }
                                        }
                                        message = message
                                                .replaceAll("原定", "預定")
                                                .replaceAll("最後班次", "尾班車")
                                                .replaceAll("尾班車已過", "尾班車已過本站");

                                        if (message.isEmpty()) {
                                            if (seq == 1) {
                                                message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                            JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.etagmb.gov.hk/eta/stop/" + stopId);
                            List<Pair<Long, JSONObject>> busList = new ArrayList<>();
                            for (int i = 0; i < data.optJSONArray("data").length(); i++) {
                                JSONObject routeData = data.optJSONArray("data").optJSONObject(i);
                                JSONArray buses = routeData.optJSONArray("eta");
                                Iterable<String> iterable = () -> DATA_SHEET.optJSONObject("routeList").keys();
                                Optional<JSONObject> filteredEntry = StreamSupport.stream(iterable.spliterator(), false)
                                        .map(k -> DATA_SHEET.optJSONObject("routeList").optJSONObject(k))
                                        .filter(r -> r.optJSONObject("bound").has("gmb") && r.optString("gtfsId").equals(routeData.optString("route_id")))
                                        .findFirst();
                                if (filteredEntry.isPresent() && buses != null) {
                                    String routeNumber = filteredEntry.get().optString("route");
                                    for (int u = 0; u < buses.length(); u++) {
                                        JSONObject bus = buses.optJSONObject(u);
                                        String eta = bus.optString("timestamp");
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                        long mins = eta.isEmpty() || eta.equalsIgnoreCase("null") ? -999 : Math.round((formatter.parse(eta, ZonedDateTime::from).toEpochSecond() - Instant.now().getEpochSecond()) / 60.0);
                                        if (routeNumber.equals(route.optString("route"))) {
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
                                        message = "<b>" + mins + "</b>" + " Min." + "";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<b>-</b>" + " Min." + "";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                    if (!remark.isEmpty()) {
                                        message += (message.isEmpty() ? remark : " (" + remark + ")");
                                    }
                                } else {
                                    if (mins > 0) {
                                        message = "<span style=\"white-space: nowrap;\"><b>" + mins + "</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    } else if (mins > -60) {
                                        message = "<span style=\"white-space: nowrap;\"><b>-</b>" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                        if (seq == 1) {
                                            nextScheduledBus = mins;
                                        }
                                    }
                                    if (!remark.isEmpty()) {
                                        message += (message.isEmpty() ? remark : " (" + remark + ")");
                                    }
                                }
                                message = message
                                        .replaceAll("原定", "預定")
                                        .replaceAll("最後班次", "尾班車")
                                        .replaceAll("尾班車已過", "尾班車已過本站");

                                if (message.isEmpty()) {
                                    if (seq == 1) {
                                        message = getNoScheduledDepartureMessage(message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
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
                            JSONArray stopsList = route.optJSONObject("stops").optJSONArray("lightRail");
                            if (JsonUtils.indexOf(stopsList, stopId) + 1 >= stopsList.length()) {
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
                                                if (routeNumber.equals(route.optString("route"))) {
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
                                        if (seq == 1) {
                                            nextScheduledBus = lrt.getEta();
                                        }
                                        StringBuilder cartsMessage = new StringBuilder(lrt.getTrainLength() * 2);
                                        for (int u = 0; u < lrt.getTrainLength(); u++) {
                                            cartsMessage.append("\uD83D\uDE83");
                                        }
                                        String message = "<b></b>" + StringUtils.getCircledNumber(lrt.getPlatformNumber()) + " " + cartsMessage + " " + minsMessage;
                                        lines.put(seq, message);
                                    }
                                }
                            }
                            break;
                        }
                        case "mtr": {
                            String lineName = route.optString("route");
                            String bound = route.optJSONObject("bound").optString("mtr");
                            if (isMtrStopEndOfLine(stopId, lineName, bound)) {
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
                                                String dest = DATA_SHEET.optJSONObject("stopList").optJSONObject(trainData.optString("dest")).optJSONObject("name").optString(Shared.Companion.getLanguage());
                                                if (!stopId.equals("AIR")) {
                                                    if (dest.equals("博覽館")) {
                                                        dest = "機場及博覽館";
                                                    } else if (dest.equals("AsiaWorld-Expo")) {
                                                        dest = "Airport & AsiaWorld-Expo";
                                                    }
                                                }
                                                if (Shared.Companion.getLanguage().equals("en")) {
                                                    dest = StringUtils.capitalize(dest);
                                                }
                                                if (!specialRoute.isEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                                    String via = DATA_SHEET.optJSONObject("stopList").optJSONObject(specialRoute).optJSONObject("name").optString(Shared.Companion.getLanguage());
                                                    if (Shared.Companion.getLanguage().equals("en")) {
                                                        via = StringUtils.capitalize(via);
                                                    }
                                                    dest += "<small>" + (Shared.Companion.getLanguage().equals("en") ? " via " : " 經") + via + "</small>";
                                                }
                                                String eta = trainData.optString("time");

                                                @SuppressLint("SimpleDateFormat")
                                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                format.setTimeZone(TimeZone.getTimeZone(hongKongTime.getZone()));
                                                long mins = (long) Math.ceil((format.parse(eta).getTime() - Instant.now().toEpochMilli()) / 60000.0);

                                                String minsMessage;
                                                if (mins > 1) {
                                                    minsMessage = mins + (Shared.Companion.getLanguage().equals("en") ? " Min." : " 分鐘");
                                                } else if (mins == 1) {
                                                    minsMessage = Shared.Companion.getLanguage().equals("en") ? "Arriving" : "即將抵達";
                                                } else {
                                                    minsMessage = Shared.Companion.getLanguage().equals("en") ? "Departing" : "正在離開";
                                                }

                                                String message = StringUtils.getCircledNumber(platform) + " " + dest + " " + minsMessage;
                                                if (seq == 1) {
                                                    nextScheduledBus = mins;
                                                    if (delayed) {
                                                        message += Shared.Companion.getLanguage().equals("en") ? " (Delayed)" : " (服務服務)";
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
                future.complete(ETAQueryResult.result(nextScheduledBus > -60 ? Math.max(0, nextScheduledBus) : -1, nextCo, lines));
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

        public static final ETAQueryResult EMPTY = new ETAQueryResult(true, -1, null, Collections.emptyMap());

        public static ETAQueryResult connectionError(String co) {
            return new ETAQueryResult(true, -1, co, Collections.singletonMap(1, Shared.Companion.getLanguage().equals("en") ? "Unable to Connect" : "無法連接伺服器"));
        }

        public static ETAQueryResult result(long nextScheduledBus, String nextCo, Map<Integer, String> lines) {
            return new ETAQueryResult(false, nextScheduledBus, nextCo, lines);
        }

        private final boolean isConnectionError;
        private final long nextScheduledBus;
        private final String nextCo;
        private final Map<Integer, String> lines;

        private ETAQueryResult(boolean isConnectionError, long nextScheduledBus, String nextCo, Map<Integer, String> lines) {
            this.isConnectionError = isConnectionError;
            this.nextScheduledBus = nextScheduledBus;
            this.nextCo = nextCo;
            this.lines = Collections.unmodifiableMap(lines);
        }

        public boolean isConnectionError() {
            return isConnectionError;
        }

        public long getNextScheduledBus() {
            return nextScheduledBus;
        }

        public String getNextCo() {
            return nextCo;
        }

        public Map<Integer, String> getLines() {
            return lines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ETAQueryResult that = (ETAQueryResult) o;
            return isConnectionError == that.isConnectionError && nextScheduledBus == that.nextScheduledBus && Objects.equals(lines, that.lines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isConnectionError, nextScheduledBus, lines);
        }
    }

    public enum State {

        LOADING, UPDATING, READY, ERROR;

    }

}
