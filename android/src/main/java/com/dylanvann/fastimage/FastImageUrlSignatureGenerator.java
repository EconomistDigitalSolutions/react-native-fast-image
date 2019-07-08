package com.dylanvann.fastimage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.ArrayList;

public class FastImageUrlSignatureGenerator {

    public static final String NAMESPACE_IMAGES = "namespace_images";

    public FastImageUrlSignatureGenerator(Context context) {
        this.context = context;
    }

    private Context context;

    private static FastImageUrlSignatureGenerator instance = null;

    public static FastImageUrlSignatureGenerator getInstance() {
        assert instance == null;

        return instance;
    }

    public static void createGenerator(Context context) {
        instance = new FastImageUrlSignatureGenerator(context);
    }

    public String getSignature(FastImagePreloaderConfiguration fastImagePreloaderConfiguration) {
        ArrayList<String> signatureParams = new ArrayList<>();

        if (fastImagePreloaderConfiguration.getNamespace() != null) {
            signatureParams.add(fastImagePreloaderConfiguration.getNamespace());
        }

        if (fastImagePreloaderConfiguration.getMaxCacheAge() > 0) {
            String maxAgeSignature = String.valueOf(System.currentTimeMillis() / (fastImagePreloaderConfiguration.getMaxCacheAge() * 1000));

            signatureParams.add(maxAgeSignature);
            signatureParams.add(String.valueOf(fastImagePreloaderConfiguration.getMaxCacheAge()));
        }

        if (signatureParams.size() > 0) {
            return TextUtils.join("|", signatureParams);
        }

        return "";
    }

    public void storeConfiguration(String url, FastImagePreloaderConfiguration fastImagePreloaderConfiguration) {
        SharedPreferences sharedPref = context.getSharedPreferences(NAMESPACE_IMAGES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        Gson gson = new Gson();
        editor.putString(url, gson.toJson(fastImagePreloaderConfiguration));
        editor.apply();
    }

    public FastImagePreloaderConfiguration getConfigurationIfAvailable(String url) {
        SharedPreferences sharedPref = context.getSharedPreferences(NAMESPACE_IMAGES, Context.MODE_PRIVATE);
        String configurationString = sharedPref.getString(url, "");

        if(configurationString.isEmpty()) {
            return null;
        }

        Gson gson = new Gson();

        return gson.fromJson(configurationString, FastImagePreloaderConfiguration.class);
    }
}
