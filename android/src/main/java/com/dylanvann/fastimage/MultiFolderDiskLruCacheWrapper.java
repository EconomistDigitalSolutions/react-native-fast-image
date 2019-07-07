package com.dylanvann.fastimage;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MultiFolderDiskLruCacheWrapper extends DiskLruCacheWrapper {

    private static final String LOG = "[FFFastImage]";

    private static Field sFieldSignatureInResourceCacheKey;
    private static Field sFieldSourceKeyInResourceCacheKey;
    private static Field sFieldSignatureInDataCacheKey;
    private static Field sFieldSourceKeyInDataCacheKey;
    private static Field sFieldObjectKey;

    static {
        try {
            Class<?> resourceCacheKeyClass = Class.forName("com.bumptech.glide.load.engine.ResourceCacheKey");
            sFieldSignatureInResourceCacheKey = resourceCacheKeyClass.getDeclaredField("signature");
            sFieldSignatureInResourceCacheKey.setAccessible(true);

            sFieldSourceKeyInResourceCacheKey = resourceCacheKeyClass.getDeclaredField("sourceKey");
            sFieldSourceKeyInResourceCacheKey.setAccessible(true);

            Class<?> objectKeyClass = Class.forName("com.bumptech.glide.signature.ObjectKey");
            sFieldObjectKey = objectKeyClass.getDeclaredField("object");
            sFieldObjectKey.setAccessible(true);

            Class<?> DataCacheKeyClass = Class.forName("com.bumptech.glide.load.engine.DataCacheKey");
            sFieldSignatureInDataCacheKey = DataCacheKeyClass.getDeclaredField("signature");
            sFieldSignatureInDataCacheKey.setAccessible(true);

            sFieldSourceKeyInDataCacheKey = DataCacheKeyClass.getDeclaredField("sourceKey");
            sFieldSourceKeyInDataCacheKey.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Log.d(LOG, "find ResourceCacheKey failed", e);
        } catch (NoSuchFieldException e) {
            Log.d(LOG, "reflect signature failed", e);
        } catch (Error error) {
            Log.d(LOG, "reflect signature failed", error);
        }
    }

    private final Context context;

    static public Map<String, DiskCache> diskCaches = new HashMap<>();
    private File directory;

    protected MultiFolderDiskLruCacheWrapper(File directory, long maxSize, Context context) {
        super(directory, maxSize);

        this.directory = directory;
        this.context = context;
    }

    @Override
    public File get(Key key) {
        return getDiskCacheBySignature(key).get(key);
    }

    @Override
    public void put(Key key, Writer writer) {
        getDiskCacheBySignature(key).put(key, writer);
    }

    @Override
    public void delete(Key key) {
        getDiskCacheBySignature(key).delete(key);
    }

    @Override
    public synchronized void clear() {
        for (DiskCache diskCache : diskCaches.values()) {
            if (diskCache != null) {
                diskCache.clear();
            }
        }
    }

    private DiskCache getDiskCacheBySignature(Key key) {
        Object cacheFolderSignature = getSignature(key);
        Object cacheFolder = null;

        if (cacheFolderSignature instanceof ObjectKey) {
            try {
                cacheFolder = sFieldObjectKey.get(cacheFolderSignature);
            } catch (IllegalAccessException e) {
                Log.d(LOG, "getSignature: " + e.getMessage());
            }
        } else {
            String sourceKey = getSourceKey(key);
            SharedPreferences sharedPref = context.getSharedPreferences("namespace_images", Context.MODE_PRIVATE);

            cacheFolder = sharedPref.getString(sourceKey, "");
        }

        return getDiskCache(cacheFolder);
    }

    @Nullable
    private Object getSignature(Key key) {
        Object signature = null;
        try {
            signature = sFieldSignatureInResourceCacheKey.get(key);
        } catch (Exception e) {
            Log.d(LOG, "getSignature: " + e.getMessage());

        }
        if (signature != null) return signature;

        try {
            signature = sFieldSignatureInDataCacheKey.get(key);
        } catch (Exception e) {
            Log.d(LOG, "getSignature: " + e.getMessage());
        }
        return signature;
    }

    @Nullable
    private String getSourceKey(Key key) {
        GlideUrl sourceKey = null;
        try {
            sourceKey = (GlideUrl) sFieldSourceKeyInResourceCacheKey.get(key);
        } catch (Exception e) {
            Log.d(LOG, "getSignature: " + e.getMessage());

        }
        if (sourceKey != null) return sourceKey.toStringUrl();

        try {
            sourceKey = (GlideUrl) sFieldSourceKeyInDataCacheKey.get(key);
        } catch (Exception e) {
            Log.d(LOG, "getSignature: " + e.getMessage());
        }
        return sourceKey.toStringUrl();
    }

    @NonNull
    private DiskCache getDiskCache(Object cacheFolder) {
        Log.d(LOG, "cacheFolder: " + cacheFolder);

        DiskCache diskCache = diskCaches.get(cacheFolder);
        if (diskCache == null) {
            String cacheFolderPath = cacheFolder instanceof EmptySignature ? "/default" : (String) cacheFolder;
            String cachePath = directory.getAbsolutePath() + cacheFolderPath;
            Log.d(LOG, "cachePath: " + cachePath);
            File fileCachePath = new File(cachePath);

            if (!fileCachePath.exists()) {
                fileCachePath.mkdirs();
            }

            try {
                diskCache = DiskLruCacheWrapper.create(fileCachePath, 1024 * 1024 * 100);

                diskCaches.put(cacheFolder.toString(), diskCache);
            } catch (Exception e) {
                Log.d(LOG, "getDiskCache: " + e.getMessage());

            }
        }
        return diskCache;
    }
}
