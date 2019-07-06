package com.dylanvann.fastimage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MultiFolderDiskLruCacheWrapper extends DiskLruCacheWrapper {

    private static final String LOG = "[FFFastImage]";

    private static Field sFieldSignatureInResourceCacheKey;
    private static Field sFieldSignatureInDataCacheKey;
    private static MultiFolderDiskLruCacheWrapper wrapper;

    static {
        try {
            Class<?> resourceCacheKeyClass = Class.forName("com.bumptech.glide.load.engine.ResourceCacheKey");
            sFieldSignatureInResourceCacheKey = resourceCacheKeyClass.getDeclaredField("signature");
            sFieldSignatureInResourceCacheKey.setAccessible(true);

            Class<?> DataCacheKeyClass = Class.forName("com.bumptech.glide.load.engine.DataCacheKey");
            sFieldSignatureInDataCacheKey = DataCacheKeyClass.getDeclaredField("signature");
            sFieldSignatureInDataCacheKey.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Log.d(LOG, "find ResourceCacheKey failed", e);
        } catch (NoSuchFieldException e) {
            Log.d(LOG, "reflect signature failed", e);
        } catch (Error error) {
            Log.d(LOG, "reflect signature failed", error);
        }
    }

    private Map<String, DiskCache> diskCaches = new HashMap<>();
    private File directory;

    public static synchronized DiskCache get() {
        if (wrapper == null) {
            wrapper = new MultiFolderDiskLruCacheWrapper(null, 0);
        }
        return wrapper;
    }

    protected MultiFolderDiskLruCacheWrapper(File directory, long maxSize) {
        super(directory, maxSize);

        this.directory = directory;
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
        Object cacheFolder = getSignature(key);

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
        if (signature != null) return  signature;

        try {
            signature = sFieldSignatureInDataCacheKey.get(key);
        } catch (Exception e) {
            Log.d(LOG, "getSignature: " + e.getMessage());
        }
        return signature;
    }

    @NonNull
    private DiskCache getDiskCache(Object cacheFolder) {
        DiskCache diskCache = diskCaches.get(cacheFolder);
        if (diskCache == null) {
            String cachePath = directory.getAbsolutePath() + cacheFolder;
            File fileCachePath = new File(cachePath);
            if (fileCachePath.mkdirs()) {
                try {
                    Class<?> clazz = Class.forName("com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper");
                    Constructor<?> ctor = clazz.getConstructor(String.class);
                    diskCache = (DiskLruCacheWrapper) ctor.newInstance(new Object[]{new File(cachePath), 1024 * 1024 * 100});

                    diskCaches.put(cacheFolder.toString(), diskCache) ;
                } catch (Exception e) {
                    Log.d(LOG, "getDiskCache: " + e.getMessage());

                }

            }
        }
        return diskCache;
    }
}
