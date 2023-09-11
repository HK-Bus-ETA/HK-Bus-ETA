package com.loohp.hkbuseta.presentation.shared;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileUpdateRequester;

import com.loohp.hkbuseta.presentation.MainActivity;
import com.loohp.hkbuseta.presentation.branchedlist.BranchedList;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceEight;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceFive;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceFour;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceOne;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceSeven;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceSix;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceThree;
import com.loohp.hkbuseta.presentation.tiles.EtaTileServiceTwo;
import com.loohp.hkbuseta.presentation.utils.ConnectionUtils;
import com.loohp.hkbuseta.presentation.utils.HTTPRequestUtils;
import com.loohp.hkbuseta.presentation.utils.IntUtils;
import com.loohp.hkbuseta.presentation.utils.JsonUtils;
import com.loohp.hkbuseta.presentation.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    private static final String KMB_ROUTE_FILE_NAME = "kmb_routes.json";
    private static final String CTB_ROUTE_FILE_NAME = "ctb_routes.json";
    private static final String NLB_ROUTE_FILE_NAME = "nlb_routes.json";
    private static final String MTR_BUS_ROUTE_FILE_NAME = "mtr_bus_routes.json";
    private static final String MTR_BUS_STOP_ALIAS_FILE_NAME = "mtr_bus_stop_alias.json";
    private static final String GMB_ROUTE_FILE_NAME = "gmb_routes.json";

    private static JSONObject PREFERENCES = null;
    private static JSONObject DATA_SHEET = null;
    private static Set<String> BUS_ROUTE = null;
    private static JSONArray KMB_ROUTE = null;
    private static JSONArray CTB_ROUTE = null;
    private static JSONArray NLB_ROUTE = null;
    private static JSONObject MTR_BUS_ROUTE = null;
    private static JSONObject MTR_BUS_STOP_ALIAS = null;
    private static JSONObject GMB_ROUTE = null;

    private volatile State state = State.LOADING;
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
        }, 0, 30000);
    }

    public State getState() {
        return state;
    }

    public boolean isPreferencesLoaded() {
        return preferencesLoaded;
    }

    public double findDistance(double lat1, double lng1, double lat2, double lng2) {
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lng1 = Math.toRadians(lng1);
        lng2 = Math.toRadians(lng2);

        double d_lon = lng2 - lng1;
        double d_lat = lat2 - lat1;
        double a = Math.pow(Math.sin(d_lat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(d_lon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return c * 6371;
    }

    private <T> boolean testOrderedSubset(JSONArray b, JSONArray a, Class<T> type) {
        return testOrderedSubset(JsonUtils.toList(b, type), JsonUtils.toList(a, type));
    }

    private <T> boolean testOrderedSubset(List<T> b, List<T> a) {
        return Collections.indexOfSubList(b, a) >= 0;
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
        if (PREFERENCES != null && DATA_SHEET != null && BUS_ROUTE != null && KMB_ROUTE != null && CTB_ROUTE != null && NLB_ROUTE != null && MTR_BUS_ROUTE != null && MTR_BUS_STOP_ALIAS != null && GMB_ROUTE != null) {
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

                boolean cached = false;
                String checksum = connectionType.hasConnection() ? HTTPRequestUtils.getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-KMB-Calculator/data/data/checksum.md5") : null;
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_SHEET_FILE_NAME) && files.contains(BUS_ROUTE_FILE_NAME) && files.contains(KMB_ROUTE_FILE_NAME) && files.contains(CTB_ROUTE_FILE_NAME) && files.contains(NLB_ROUTE_FILE_NAME) && files.contains(MTR_BUS_ROUTE_FILE_NAME) && files.contains(MTR_BUS_STOP_ALIAS_FILE_NAME) && files.contains(GMB_ROUTE_FILE_NAME)) {
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
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(KMB_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        KMB_ROUTE = new JSONArray(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(CTB_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        CTB_ROUTE = new JSONArray(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(NLB_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        NLB_ROUTE = new JSONArray(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(MTR_BUS_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        MTR_BUS_ROUTE = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(MTR_BUS_STOP_ALIAS_FILE_NAME), StandardCharsets.UTF_8))) {
                        MTR_BUS_STOP_ALIAS = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(GMB_ROUTE_FILE_NAME), StandardCharsets.UTF_8))) {
                        GMB_ROUTE = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    state = State.READY;
                } else if (!connectionType.hasConnection()) {
                    state = State.ERROR;
                } else {
                    state = State.UPDATING;

                    BUS_ROUTE = new LinkedHashSet<>();

                    KMB_ROUTE = HTTPRequestUtils.getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/route/").optJSONArray("data");
                    for (int i = 0; i < KMB_ROUTE.length(); i++) {
                        String route = KMB_ROUTE.optJSONObject(i).optString("route");
                        BUS_ROUTE.add(route);
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(KMB_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(KMB_ROUTE.toString());
                        pw.flush();
                    }

                    CTB_ROUTE = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/route/ctb").optJSONArray("data");
                    for (int i = 0; i < CTB_ROUTE.length(); i++) {
                        String route = CTB_ROUTE.optJSONObject(i).optString("route");
                        BUS_ROUTE.add(route);
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(CTB_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(CTB_ROUTE.toString());
                        pw.flush();
                    }

                    NLB_ROUTE = HTTPRequestUtils.getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/route.php?action=list").optJSONArray("routes");
                    for (int i = 0; i < NLB_ROUTE.length(); i++) {
                        String route = NLB_ROUTE.optJSONObject(i).optString("routeNo");
                        BUS_ROUTE.add(route);
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(NLB_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(NLB_ROUTE.toString());
                        pw.flush();
                    }

                    MTR_BUS_STOP_ALIAS = HTTPRequestUtils.getJSONResponse("https://raw.githubusercontent.com/LOOHP/HK-KMB-Calculator/data/data/mtr_bus_stop_alias.json");
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(MTR_BUS_STOP_ALIAS_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(MTR_BUS_STOP_ALIAS.toString());
                        pw.flush();
                    }

                    GMB_ROUTE = HTTPRequestUtils.getJSONResponse("https://data.etagmb.gov.hk/route").optJSONObject("data").optJSONObject("routes");
                    for (Iterator<String> itr = GMB_ROUTE.keys(); itr.hasNext(); ) {
                        String region = itr.next();
                        JSONArray list = GMB_ROUTE.optJSONArray(region);
                        for (int i = 0; i < list.length(); i++) {
                            String route = list.optString(i);
                            BUS_ROUTE.add(route);
                        }
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(GMB_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(GMB_ROUTE.toString());
                        pw.flush();
                    }

                    DATA_SHEET = HTTPRequestUtils.getJSONResponse("https://raw.githubusercontent.com/LOOHP/hk-bus-crawling/gh-pages/routeFareList.min.json");

                    try {
                        JSONObject a = DATA_SHEET.optJSONObject("stopList").optJSONObject("AC1FD9BDD09D1DD6").optJSONObject("name");
                        a.put("zh", a.optString("zh") + " (沙頭角邊境禁區 - 需持邊境禁區許可證)");
                        a.put("en", a.optString("en") + " (Sha Tau Kok Frontier Closed Area - Closed Area Permit Required)");

                        JSONObject b = DATA_SHEET.optJSONObject("stopList").optJSONObject("20001477").optJSONObject("name");
                        b.put("zh", b.optString("zh") + " (沙頭角邊境禁區 - 需持邊境禁區許可證)");
                        b.put("en", b.optString("en") + " (Sha Tau Kok Frontier Closed Area - Closed Area Permit Required)");

                        JSONObject c1 = DATA_SHEET.optJSONObject("stopList").optJSONObject("152").optJSONObject("name");
                        c1.put("zh", c1.optString("zh") + " (深圳灣管制站 - 僅限過境旅客)");
                        c1.put("en", c1.optString("en") + " (Shenzhen Bay Control Point - Border Crossing Passengers Only)");

                        JSONObject c2 = DATA_SHEET.optJSONObject("stopList").optJSONObject("20015453").optJSONObject("name");
                        c2.put("zh", c2.optString("zh") + " (深圳灣管制站 - 僅限過境旅客)");
                        c2.put("en", c2.optString("en") + " (Shenzhen Bay Control Point - Border Crossing Passengers Only)");

                        JSONObject c3 = DATA_SHEET.optJSONObject("stopList").optJSONObject("003208").optJSONObject("name");
                        c3.put("zh", c3.optString("zh") + " (深圳灣管制站 - 僅限過境旅客)");
                        c3.put("en", c3.optString("en") + " (Shenzhen Bay Control Point - Border Crossing Passengers Only)");

                        JSONObject d1 = DATA_SHEET.optJSONObject("stopList").optJSONObject("81567ACCCF40DD4B").optJSONObject("name");
                        d1.put("zh", d1.optString("zh") + " (落馬洲支線出入境管制站 - 僅限過境旅客)");
                        d1.put("en", d1.optString("en") + " (Lok Ma Chau Spur Line Immigration Control Point - Border Crossing Passengers Only)");

                        JSONObject d2 = DATA_SHEET.optJSONObject("stopList").optJSONObject("20015420").optJSONObject("name");
                        d2.put("zh", d2.optString("zh") + " (落馬洲支線出入境管制站 - 僅限過境旅客)");
                        d2.put("en", d2.optString("en") + " (Lok Ma Chau Spur Line Immigration Control Point - Border Crossing Passengers Only)");

                        JSONObject e1 = DATA_SHEET.optJSONObject("stopList").optJSONObject("20011698").optJSONObject("name");
                        e1.put("zh", e1.optString("zh") + " (落馬洲管制站 - 僅限過境旅客)");
                        e1.put("en", e1.optString("en") + " (Lok Ma Chau Control Point - Border Crossing Passengers Only)");

                        JSONObject e2 = DATA_SHEET.optJSONObject("stopList").optJSONObject("20015598").optJSONObject("name");
                        e2.put("zh", e2.optString("zh") + " (落馬洲管制站 - 僅限過境旅客)");
                        e2.put("en", e2.optString("en") + " (Lok Ma Chau Control Point - Border Crossing Passengers Only)");

                        Set<String> kmbOps = new HashSet<>();

                        Set<String> keysToRemove = new HashSet<>();
                        //outer:
                        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            /*
                            if (keysToRemove.contains(key)) {
                                continue;
                            }
                            */
                            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                            /*
                            String co = data.optJSONObject("bound").has("kmb") ? "kmb" :
                                    (data.optJSONObject("bound").has("ctb") ? "ctb" :
                                            (data.optJSONObject("bound").has("nlb") ? "nlb" :
                                                    (data.optJSONObject("bound").has("mtr-bus") ? "mtr-bus" : "gmb")));

                            if (!co.equals("ctb")) {
                                JSONArray stops = data.optJSONObject("stops").optJSONArray(co);
                                for (Iterator<String> it = DATA_SHEET.optJSONObject("routeList").keys(); it.hasNext(); ) {
                                    String key0 = it.next();
                                    if (keysToRemove.contains(key0)) {
                                        continue;
                                    }
                                    JSONObject data0 = DATA_SHEET.optJSONObject("routeList").optJSONObject(key0);
                                    String co0 = data0.optJSONObject("bound").has("kmb") ? "kmb" :
                                            (data0.optJSONObject("bound").has("ctb") ? "ctb" :
                                                    (data0.optJSONObject("bound").has("nlb") ? "nlb" :
                                                            (data0.optJSONObject("bound").has("mtr-bus") ? "mtr-bus" : "gmb")));

                                    if (!data.equals(data0) && co.equals(co0) && data.optString("route").equals(data0.optString("route"))) {
                                        JSONArray stops0 = data0.optJSONObject("stops").optJSONArray(co0);
                                        if (stops != null && stops0 != null && testOrderedSubset(stops0, stops, String.class)) {
                                            keysToRemove.add(key);
                                            continue outer;
                                        }
                                    }
                                }
                            }
                            */

                            if (data.optJSONObject("bound").has("kmb")) {
                                if (data.optJSONArray("co").toString().contains("ctb")) {
                                    kmbOps.add(data.optString("route"));
                                }
                                /*
                                if (!data.has("fares")) {
                                    for (Iterator<String> it = DATA_SHEET.optJSONObject("routeList").keys(); it.hasNext(); ) {
                                        String key0 = it.next();
                                        if (keysToRemove.contains(key0)) {
                                            continue;
                                        }
                                        JSONObject data0 = DATA_SHEET.optJSONObject("routeList").optJSONObject(key0);
                                        if (!data.equals(data0) && data0.has("fares") &&
                                                data0.optString("route").equals(data.optString("route")) &&
                                                data0.getInt("seq") == data.getInt("seq")) {
                                            JSONArray fares = data0.optJSONArray("fares");
                                            if (fares != null) {
                                                int length = Math.min(fares.length(), data.optJSONObject("stops").optJSONArray("kmb").length());
                                                data.put("fares", new JSONArray(JsonUtils.toList(fares, String.class).subList(0, length)));
                                            }
                                        }
                                    }
                                }
                                */
                            }
                        }
                        /*
                        for (String key : keysToRemove) {
                            DATA_SHEET.optJSONObject("routeList").remove(key);
                        }
                        */

                        Set<String> ctbOps = new HashSet<>();
                        Map<String, JSONArray> ctbStopCircularSets = new HashMap<>();
                        Map<String, JSONArray> ctbStopSets = new HashMap<>();

                        //keysToRemove.clear();
                        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                            if (data.optJSONObject("bound").has("ctb")) {
                                if (kmbOps.contains(data.optString("route")) && !data.optJSONObject("bound").has("kmb")) {
                                    ctbOps.add(data.optString("route"));
                                    keysToRemove.add(key);
                                } else {
                                    if (data.optJSONObject("bound").optString("ctb").length() > 1 &&
                                            data.optJSONObject("orig").optString("zh").equals(data.optJSONObject("dest").optString("zh"))) {
                                        String routeKey = data.optString("route") + "," + data.getInt("serviceType");
                                        JSONArray stops = data.optJSONObject("stops").optJSONArray("ctb");
                                        if (ctbStopCircularSets.containsKey(routeKey)) {
                                            if (ctbStopCircularSets.get(routeKey).length() < stops.length()) {
                                                ctbStopCircularSets.put(routeKey, stops);
                                            }
                                        } else {
                                            ctbStopCircularSets.put(routeKey, stops);
                                        }
                                    } else {
                                        if (data.optJSONObject("bound").optString("ctb").length() > 1) {
                                            data.optJSONObject("bound").put("ctb", "I");
                                        }
                                        String routeKey = data.optString("route") + "," + data.optJSONObject("bound").optString("ctb") + ","
                                                + data.getInt("serviceType");
                                        JSONArray stops = data.optJSONObject("stops").optJSONArray("ctb");
                                        if (ctbStopSets.containsKey(routeKey)) {
                                            if (ctbStopSets.get(routeKey).length() < stops.length()) {
                                                ctbStopSets.put(routeKey, stops);
                                            }
                                        } else {
                                            ctbStopSets.put(routeKey, stops);
                                        }
                                    }
                                }
                            }
                        }
                        for (String key : keysToRemove) {
                            DATA_SHEET.optJSONObject("routeList").remove(key);
                        }

                        Map<String, JSONArray> ctbRouteDests = new HashMap<>();

                        keysToRemove.clear();
                        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                            if (data.optJSONObject("bound").has("ctb")) {
                                String routeKey = data.optString("route") + "," + data.getInt("serviceType");
                                if (ctbStopCircularSets.containsKey(routeKey)) {
                                    JSONArray stops = data.optJSONObject("stops").optJSONArray("ctb");
                                    JSONArray longestStops = ctbStopCircularSets.get(routeKey);
                                    if (!stops.equals(longestStops) && testOrderedSubset(longestStops, stops, String.class)) {
                                        if (ctbRouteDests.containsKey(routeKey)) {
                                            ctbRouteDests.get(routeKey).put(data.optJSONObject("dest"));
                                        } else {
                                            JSONArray destArray = new JSONArray();
                                            destArray.put(data.optJSONObject("dest"));
                                            ctbRouteDests.put(routeKey, destArray);
                                        }
                                        keysToRemove.add(key);
                                        continue;
                                    }
                                    if (data.optJSONObject("bound").optString("ctb").length() > 1) {
                                        data.optJSONObject("bound").put("ctb", "O");
                                    }
                                    if (!stops.equals(longestStops) && data.getInt("serviceType") == 1) {
                                        data.put("ctbSpecial", "Any");
                                    }
                                } else {
                                    routeKey = data.optString("route") + "," + data.optJSONObject("bound").optString("ctb") + ","
                                            + data.getInt("serviceType");
                                    if (ctbStopSets.containsKey(routeKey)) {
                                        JSONArray stops = data.optJSONObject("stops").optJSONArray("ctb");
                                        JSONArray longestStops = ctbStopSets.get(routeKey);
                                        if (!stops.equals(longestStops)) {
                                            data.put("ctbSpecial", data.optJSONObject("bound").optString("ctb"));
                                        }
                                    }
                                }
                            }
                        }
                        for (String key : keysToRemove) {
                            DATA_SHEET.optJSONObject("routeList").remove(key);
                        }

                        Set<String> intersection = new HashSet<>(ctbOps);
                        intersection.retainAll(kmbOps);

                        for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                            if (intersection.contains(data.optString("route"))) {
                                data.put("jointOp", true);
                            }
                            if (data.optJSONObject("bound").has("ctb")) {
                                String routeKey = data.optString("route") + "," + data.getInt("serviceType");
                                if (ctbRouteDests.containsKey(routeKey)) {
                                    JSONArray destList = ctbRouteDests.get(routeKey);
                                    for (int i = destList.length() - 1; i >= 0; i--) {
                                        JSONObject e = destList.optJSONObject(i);
                                        if (e.optString("zh").equals(data.optJSONObject("dest").optString("zh")) && e.optString("en").equals(data.optJSONObject("dest").optString("en"))) {
                                            destList.remove(i);
                                        }
                                    }
                                    if (destList.length() > 0) {
                                        data.put("ctbCircularDests", destList);
                                    }
                                }
                            }
                        }

                        MTR_BUS_ROUTE = HTTPRequestUtils.getJSONResponse("https://raw.githubusercontent.com/LOOHP/HK-KMB-Calculator/data/data/mtr_bus_routes.json");
                        for (Iterator<String> itr = MTR_BUS_ROUTE.keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            JSONObject data = MTR_BUS_ROUTE.optJSONObject(key);
                            String route = data.optString("route");
                            DATA_SHEET.optJSONObject("routeList").put(key, data);
                            BUS_ROUTE.add(route);
                        }
                        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(MTR_BUS_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                            pw.write(MTR_BUS_ROUTE.toString());
                            pw.flush();
                        }

                        JSONObject mtrBusStops = HTTPRequestUtils.getJSONResponse("https://raw.githubusercontent.com/LOOHP/HK-KMB-Calculator/data/data/mtr_bus_stops.json");
                        for (Iterator<String> itr = mtrBusStops.keys(); itr.hasNext(); ) {
                            String key = itr.next();
                            DATA_SHEET.optJSONObject("stopList").put(key, mtrBusStops.opt(key));
                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(DATA_SHEET_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(DATA_SHEET.toString());
                        pw.flush();
                    }
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(BUS_ROUTE_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                        pw.write(JsonUtils.fromCollection(BUS_ROUTE).toString());
                        pw.flush();
                    }
                }
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(CHECKSUM_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                    pw.write(checksum == null ? "" : checksum);
                    pw.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

    public Pair<Set<Character>, Boolean> getPossibleNextChar(String input) {
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
        return Pair.create(result, exactMatch);
    }

    public List<JSONObject> findRoutes(String input) {
        try {
            Map<String, JSONObject> matchingRoutes = new HashMap<>();

            for (Iterator<String> itr = DATA_SHEET.optJSONObject("routeList").keys(); itr.hasNext(); ) {
                String key = itr.next();
                JSONObject data = DATA_SHEET.optJSONObject("routeList").optJSONObject(key);
                if (data.optString("route").equals(input)) {
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
                String coAStr = boundA.has("kmb") ? "kmb" : (boundA.has("ctb") ? "ctb" : (boundA.has("nlb") ? "nlb" : (boundA.has("mtr-bus") ? "mtr-bus" : "gmb")));
                String coBStr = boundB.has("kmb") ? "kmb" : (boundB.has("ctb") ? "ctb" : (boundB.has("nlb") ? "nlb" : (boundB.has("mtr-bus") ? "mtr-bus" : "gmb")));
                int coA = coAStr.equals("kmb") ? 0 : (coAStr.equals("ctb") ? 1 : (coAStr.equals("nlb") ? 2 : (coAStr.equals("mtr-bus") ? 3 : 4)));
                int coB = coBStr.equals("kmb") ? 0 : (coBStr.equals("ctb") ? 1 : (coBStr.equals("nlb") ? 2 : (coBStr.equals("mtr-bus") ? 3 : 4)));
                if (coA != coB) {
                    return coA - coB;
                }
                if (coA == 2) {
                    return IntUtils.parseOrZero(a.optJSONObject("route").optString("nlbId")) - IntUtils.parseOrZero(b.optJSONObject("route").optString("nlbId"));
                } else {
                    if (coA == 4) {
                        int gtfsDiff = IntUtils.parseOrZero(a.optJSONObject("route").optString("gtfsId")) - IntUtils.parseOrZero(b.optJSONObject("route").optString("gtfsId"));
                        if (gtfsDiff != 0) {
                            return gtfsDiff;
                        }
                    }
                    int typeDiff = IntUtils.parseOrZero(a.optJSONObject("route").optString("serviceType")) - IntUtils.parseOrZero(b.optJSONObject("route").optString("serviceType"));
                    if (typeDiff == 0) {
                        if (coA == 1) {
                            return Boolean.compare(a.optJSONObject("route").has("ctbSpecial"), b.optJSONObject("route").has("ctbSpecial"));
                        }
                        return -boundA.optString(coAStr).compareTo(boundB.optString(coBStr));
                    } else {
                        return typeDiff;
                    }
                }
            });
            return routes;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public NearbyRoutesResult getNearbyRoutes(double lat, double lng, Set<String> excludedRouteNumbers) {
        try {
            //lat = 22.475977635712525;
            //lng = 114.15532485241508;

            JSONObject stops = DATA_SHEET.optJSONObject("stopList");
            List<JSONObject> nearbyStops = new ArrayList<>();

            JSONObject closestStop = null;
            double closestDistance = Double.MAX_VALUE;

            for (Iterator<String> itr = stops.keys(); itr.hasNext(); ) {
                String stopId = itr.next();
                JSONObject entry = stops.optJSONObject(stopId);
                JSONObject location = entry.optJSONObject("location");
                double distance = findDistance(lat, lng, location.optDouble("lat"), location.optDouble("lng"));

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

                    boolean isKmb = data.has("bound") && data.optJSONObject("bound").has("kmb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("kmb"), stopId);
                    boolean isCtb = data.has("bound") && data.optJSONObject("bound").has("ctb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("ctb"), stopId);
                    boolean isNlb = data.has("bound") && data.optJSONObject("bound").has("nlb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("nlb"), stopId);
                    boolean isMtrBus = data.has("bound") && data.optJSONObject("bound").has("mtr-bus") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("mtr-bus"), stopId);
                    boolean isGmb = data.has("bound") && data.optJSONObject("bound").has("gmb") && JsonUtils.contains(data.optJSONObject("stops").optJSONArray("gmb"), stopId);

                    if (isKmb || isCtb || isNlb || isMtrBus || isGmb) {
                        String co = isKmb ? "kmb" : (isCtb ? "ctb" : (isNlb ? "nlb" : (isMtrBus ? "mtr-bus" : "gmb")));
                        String key0 = data.optString("route") + "," + co + "," + (co.equals("nlb") ? data.optString("nlbId") : data.optJSONObject("bound").optString(co));

                        JSONObject location = nearbyStop.optJSONObject("data").optJSONObject("location");
                        if (nearbyRoutes.containsKey(key0)) {
                            JSONObject existingNearbyRoute = nearbyRoutes.get(key0);

                            if (existingNearbyRoute.optJSONObject("stop").optDouble("distance") > nearbyStop.optDouble("distance")) {
                                try {
                                    int type = Integer.parseInt(data.optString("serviceType"));
                                    int matchingType = Integer.parseInt(existingNearbyRoute.optJSONObject("route").optString("serviceType"));

                                    if (type < matchingType) {
                                        existingNearbyRoute.put("stop", nearbyStop);
                                        existingNearbyRoute.put("route", data);
                                        existingNearbyRoute.put("co", co);
                                        existingNearbyRoute.put("origin", location);
                                    }
                                } catch (NumberFormatException ignore) {
                                    existingNearbyRoute.put("stop", nearbyStop);
                                    existingNearbyRoute.put("route", data);
                                    existingNearbyRoute.put("co", co);
                                    existingNearbyRoute.put("origin", location);
                                }
                            }
                        } else {
                            JSONObject newNearbyRoute = new JSONObject();
                            newNearbyRoute.put("stop", nearbyStop);
                            newNearbyRoute.put("route", data);
                            newNearbyRoute.put("co", co);
                            newNearbyRoute.put("origin", location);
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
                String pa = String.valueOf(a.optJSONObject("route").optString("route").charAt(0));
                String sa = String.valueOf(a.optJSONObject("route").optString("route").charAt(a.optJSONObject("route").optString("route").length() - 1));
                int na = IntUtils.parseOrZero(a.optJSONObject("route").optString("route").replaceAll("[^0-9]", ""));

                if (a.optJSONObject("route").optJSONObject("bound").has("gmb")) {
                    na += 1000;
                }
                if (pa.equals("N") || a.optJSONObject("route").optString("route").equals("270S") || a.optJSONObject("route").optString("route").equals("271S") || a.optJSONObject("route").optString("route").equals("293S") || a.optJSONObject("route").optString("route").equals("701S") || a.optJSONObject("route").optString("route").equals("796S")) {
                    na -= (isNight ? 1 : -1) * 10000;
                }
                if (sa.equals("S") && !a.optJSONObject("route").optString("route").equals("89S") && !a.optJSONObject("route").optString("route").equals("796S")) {
                    na += 1000;
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
            }));

            return new NearbyRoutesResult(routes, closestStop, closestDistance);
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
        BranchedList<String, StopData> result = new BranchedList<>((a, b) -> a.getServiceType() > b.getServiceType() ? b : a);
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
                    for (int i = 0; i < stops.length(); i++) {
                        String stopId = stops.optString(i);
                        int serviceType = IntUtils.parseOr(route.optString("serviceType"), 1);
                        localStops.add(stopId, new StopData(stopId, serviceType, DATA_SHEET.optJSONObject("stopList").optJSONObject(stopId)));
                    }
                    result.merge(localStops);
                }
            }
        }
        return result.values();
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

    public static String getNoScheduledDepartureMessage(int elementFontSize, String altMessage, boolean isAboveTyphoonSignalEight, String typhoonWarningTitle) {
        if (altMessage == null || altMessage.isEmpty()) {
            altMessage = Shared.Companion.getLanguage().equals("en") ? "No scheduled departures at this moment" : "暫時沒有預定班次";
        }
        if (isAboveTyphoonSignalEight) {
            altMessage += " (" + typhoonWarningTitle + ")";
        }
        if (isAboveTyphoonSignalEight) {
            return "<span style=\"font-size: " + elementFontSize + "px;\"></span><span style=\"color: #6472BC;\">" + altMessage + "</span>";
        } else {
            return "<span style=\"font-size: " + elementFontSize + "px;\"></span>" + altMessage;
        }
    }

    public static ETAQueryResult getEta(String stopId, String co, JSONObject route, Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return ETAQueryResult.CONNECTION_ERROR;
        }
        int elementFontSize = 25;
        CompletableFuture<ETAQueryResult> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Map<Integer, String> lines = new HashMap<>();
                long nextScheduledBus = -999;
                lines.put(1, getNoScheduledDepartureMessage(elementFontSize, null, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle()));
                String language = Shared.Companion.getLanguage();
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
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        }
                                        if (!bus.optString("rmk_en").isEmpty()) {
                                            message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                        }
                                    } else {
                                        if (mins > 0) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
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
                                            message = getNoScheduledDepartureMessage(elementFontSize, message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
                                        } else {
                                            message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>-";
                                        }
                                    } else {
                                        message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>" + message;
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
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        }
                                        if (!bus.optString("rmk_en").isEmpty()) {
                                            message += (message.isEmpty() ? bus.optString("rmk_en") : " (" + bus.optString("rmk_en") + ")");
                                        }
                                    } else {
                                        if (mins > 0) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
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
                                            message = getNoScheduledDepartureMessage(elementFontSize, message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
                                        } else {
                                            message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>-";
                                        }
                                    } else {
                                        message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>" + message;
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
                                    message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " Min." + "";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                } else if (mins > -60) {
                                    message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " Min." + "";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                }
                                if (!variant.isEmpty()) {
                                    message += (message.isEmpty() ? variant : " (" + variant + ")");
                                }
                            } else {
                                if (mins > 0) {
                                    message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                } else if (mins > -60) {
                                    message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
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
                                    message = getNoScheduledDepartureMessage(elementFontSize, message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
                                } else {
                                    message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>-";
                                }
                            } else {
                                message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>" + message;
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
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " Min." + "";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        }
                                        if (!remark.isEmpty()) {
                                            message += (message.isEmpty() ? remark : " (" + remark + ")");
                                        }
                                    } else {
                                        if (mins > 0) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                            if (seq == 1) {
                                                nextScheduledBus = mins;
                                            }
                                        } else if (mins > -60) {
                                            message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
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
                                            message = getNoScheduledDepartureMessage(elementFontSize, message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
                                        } else {
                                            message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>-";
                                        }
                                    } else {
                                        message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>" + message;
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
                                    message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " Min." + "";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                } else if (mins > -60) {
                                    message = "" + "<b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " Min." + "";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                }
                                if (!remark.isEmpty()) {
                                    message += (message.isEmpty() ? remark : " (" + remark + ")");
                                }
                            } else {
                                if (mins > 0) {
                                    message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">" + mins + "</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
                                    if (seq == 1) {
                                        nextScheduledBus = mins;
                                    }
                                } else if (mins > -60) {
                                    message = "<span style=\"white-space: nowrap;\"><b style=\"font-size: " + elementFontSize + "px;\">-</b>" + "" + " <span style=\"word-break: keep-all;\">分鐘</span></span>";
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
                                    message = getNoScheduledDepartureMessage(elementFontSize, message, INSTANCE.isAboveTyphoonSignalEight(), INSTANCE.getTyphoonWarningTitle());
                                } else {
                                    message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>-";
                                }
                            } else {
                                message = "<b style=\"font-size: " + elementFontSize + "px;\"></b>" + message;
                            }
                            lines.put(seq, message);
                        }
                        break;
                    }
                }
                future.complete(ETAQueryResult.result(nextScheduledBus > -60 ? Math.max(0, nextScheduledBus) : -1, lines));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }).start();
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return ETAQueryResult.CONNECTION_ERROR;
        }
    }

    public static class ETAQueryResult {

        public static final ETAQueryResult EMPTY = new ETAQueryResult(true, -1, Collections.emptyMap());

        public static final ETAQueryResult CONNECTION_ERROR = new ETAQueryResult(true, -1, Collections.singletonMap(1, Shared.Companion.getLanguage().equals("en") ? "Unable to Connect" : "無法連接伺服器"));

        public static ETAQueryResult result(long nextScheduledBus, Map<Integer, String> lines) {
            return new ETAQueryResult(false, nextScheduledBus, lines);
        }

        private final boolean isConnectionError;
        private final long nextScheduledBus;
        private final Map<Integer, String> lines;

        private ETAQueryResult(boolean isConnectionError, long nextScheduledBus, Map<Integer, String> lines) {
            this.isConnectionError = isConnectionError;
            this.nextScheduledBus = nextScheduledBus;
            this.lines = Collections.unmodifiableMap(lines);
        }

        public boolean isConnectionError() {
            return isConnectionError;
        }

        public long getNextScheduledBus() {
            return nextScheduledBus;
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
