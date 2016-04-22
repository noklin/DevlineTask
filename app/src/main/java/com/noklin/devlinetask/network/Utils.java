package com.noklin.devlinetask.network;

import android.util.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;



public class Utils {

    public static HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Authorization", "basic " +
                Base64.encodeToString("admin:".getBytes(), Base64.NO_WRAP));
        connection.connect();
        return  connection;
    }
}

