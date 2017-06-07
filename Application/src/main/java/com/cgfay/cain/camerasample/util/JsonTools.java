package com.cgfay.cain.camerasample.util;


import android.util.Log;

import com.cgfay.cain.camerasample.model.Striker;
import com.google.gson.Gson;

public class JsonTools {

    private static final String TAG = "JsonTools";

    private JsonTools() {}

    public static Striker parseFromJson(String jsonString) {
        Gson json = new Gson();
        Striker striker = json.fromJson(jsonString, Striker.class);
        Log.d(TAG, striker.toString());
        return striker;
    }
}
