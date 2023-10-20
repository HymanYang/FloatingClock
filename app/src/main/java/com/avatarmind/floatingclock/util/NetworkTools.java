package com.avatarmind.floatingclock.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NetworkTools {

    static OkHttpClient client = new OkHttpClient();

    public static void get(String url, okhttp3.Callback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

}
