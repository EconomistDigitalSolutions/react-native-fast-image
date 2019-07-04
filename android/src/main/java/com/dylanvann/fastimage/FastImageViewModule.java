package com.dylanvann.fastimage;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class FastImageViewModule extends ReactContextBaseJavaModule {
    private static final String LOG = "[FFFastImage]";

    private static final String REACT_CLASS = "FastImageView";

    public FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void addReadOnlyCachePath(String path) {
        Log.d(LOG, "addReadOnlyCachePath");
    }
}
