package com.loohp.hkbuseta.utils;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HTTPRequestUtils {

    public static String getMovedRedirect(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_MOVED_PERM || connection.getResponseCode() == HttpsURLConnection.HTTP_MOVED_TEMP) {
                return connection.getHeaderField("Location");
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

}
