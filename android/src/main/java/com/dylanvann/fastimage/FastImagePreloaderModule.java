package com.dylanvann.fastimage;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

class FastImagePreloaderModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImagePreloaderManager";
    private int preloaders = 0;
    private Map<Integer, FastImagePreloaderConfiguration> fastImagePreloaders = new HashMap<>();

    private static final String LOG = "[FFFastImage]";


    FastImagePreloaderModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void createPreloader(Promise promise) {
        promise.resolve(preloaders++);
    }

    @ReactMethod
    public void createPreloaderWithConfig(ReadableMap preloadConfig, Promise promise) {
        preloaders++;

        if (preloadConfig == null) {
            fastImagePreloaders.put(preloaders,
                    new FastImagePreloaderConfiguration()
            );
        } else {
            fastImagePreloaders.put(preloaders,
                    new FastImagePreloaderConfiguration(preloadConfig.getString("namespace"), preloadConfig.getInt("maxCacheAge"))
            );
        }


        promise.resolve(preloaders);
    }

    @ReactMethod
    public void preload(final int preloaderId, final ReadableArray sources, final String cacheControl) {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FastImagePreloaderListener preloader = new FastImagePreloaderListener(getReactApplicationContext(), preloaderId, sources.size());
                FastImagePreloaderConfiguration fastImagePreloaderConfiguration = fastImagePreloaders.get(preloaderId);

                for (int i = 0; i < sources.size(); i++) {
                    final ReadableMap source = sources.getMap(i);
                    final FastImageSource imageSource = FastImageViewConverter.getImageSource(activity, source);
                    final Object resource = imageSource.isBase64Resource() ? imageSource.getSource() :
                            imageSource.isResource() ? imageSource.getUri() : imageSource.getGlideUrl();

                    RequestBuilder requestBuilder = Glide
                            .with(activity.getApplicationContext())
                            .downloadOnly()
                            .load(resource)
                            .listener(preloader);

                    String objectSignature = FastImageUrlSignatureGenerator.getInstance().getSignature(fastImagePreloaderConfiguration);

                    if (!objectSignature.isEmpty()) {
                        requestBuilder = requestBuilder.apply(new RequestOptions()
                                .signature(new ObjectKey(objectSignature))
                        );

                        String url = resource instanceof GlideUrl ? ((GlideUrl) resource).toStringUrl() : (String) resource;

                        FastImageUrlSignatureGenerator.getInstance().storeConfiguration(url, fastImagePreloaderConfiguration);
                    }


                    requestBuilder.apply(FastImageViewConverter.getOptions(source))
                            .preload();
                }
            }
        });
    }

    @ReactMethod
    public void remove(final String namespace) {
        MultiFolderDiskLruCacheWrapper.diskCaches.remove(namespace);

        final Activity activity = getCurrentActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.get(activity).clearMemory();
            }
        });
    }
}
