package com.dylanvann.fastimage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MultiFolderDiskLruCacheWrapper extends DiskLruCacheWrapper {

    private static final String DEFAULT_CACHE = "default";


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
        } catch (ClassNotFoundException ignored) {
        } catch (NoSuchFieldException ignored) {
        } catch (Error ignored) {
        }
    }

    static public Map<String, DiskCache> diskCaches = new HashMap<>();
    private File directory;

    protected MultiFolderDiskLruCacheWrapper(File directory, long maxSize) {
        super(directory, maxSize);

        this.directory = directory;
    }

    @Override
    public File get(Key key) {
        DiskCache cache = getDiskCacheBySignature(key);

        return cache.get(key);
    }

    @Override
    public void put(Key key, Writer writer) {
        DiskCache cache = getDiskCacheBySignature(key);

        cache.put(key, writer);
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

    @Nullable
    private Object getSignature(Key key) {
        Object signature = null;
        try {
            signature = sFieldSignatureInResourceCacheKey.get(key);
        } catch (Exception ignored) {
        }
        if (signature != null) return signature;

        try {
            signature = sFieldSignatureInDataCacheKey.get(key);
        } catch (Exception ignored) {
        }
        return signature;
    }

    @Nullable
    private String getSourceKey(Key key) {
        GlideUrl sourceKey = null;
        try {
            sourceKey = (GlideUrl) sFieldSourceKeyInResourceCacheKey.get(key);
        } catch (Exception ignored) {
        }
        if (sourceKey != null) return sourceKey.toStringUrl();

        try {
            sourceKey = (GlideUrl) sFieldSourceKeyInDataCacheKey.get(key);
        } catch (Exception ignored) {
        }
        return sourceKey != null ? sourceKey.toStringUrl() : "";
    }

    private DiskCache getDiskCacheBySignature(Key key) {
        Object cacheFolderSignature = getSignature(key);
        String cacheIdentifier = getSourceKey(key);
        FastImagePreloaderConfiguration configuration = FastImageUrlSignatureGenerator.getInstance().fetchConfiguration(cacheIdentifier);

        if (cacheFolderSignature instanceof ObjectKey || configuration != null) {
            return getDiskCache(cacheIdentifier, configuration);

        }

        return getDefaultDiskCache();
    }

    @NonNull
    private DiskCache getDiskCache(String cacheIdentifier, FastImagePreloaderConfiguration configuration) {
        DiskCache diskCache = diskCaches.get(configuration.getNamespace());

        if (diskCache == null) {
            String relativeCacheFolderPath = getRelativeCachePath(cacheIdentifier);

            String cachePath = directory.getAbsolutePath() + relativeCacheFolderPath;

            diskCache = createNewDiskCache(cachePath, configuration.getNamespace());
        }

        return diskCache;
    }

    @NonNull
    private DiskCache getDefaultDiskCache() {
        DiskCache diskCache = diskCaches.get(DEFAULT_CACHE);

        if (diskCache == null) {
            String relativeCacheFolderPath = getRelativeCachePath(DEFAULT_CACHE);
            String cachePath = directory.getAbsolutePath() + relativeCacheFolderPath;

            diskCache = createNewDiskCache(cachePath, DEFAULT_CACHE);
        }

        return diskCache;
    }

    private DiskCache createNewDiskCache(String cachePath, String cacheIdentifier) {
        DiskCache diskCache;

        diskCache = DiskLruCacheWrapper.create(new File(cachePath), 1024 * 1024 * 300);

        diskCaches.put(cacheIdentifier, diskCache);

        return diskCache;
    }

    private String getRelativeCachePath(String cacheIdentifier) {
        if (cacheIdentifier.equals(DEFAULT_CACHE)) {
            return "/default";
        }

        FastImagePreloaderConfiguration configuration = FastImageUrlSignatureGenerator.getInstance().fetchConfiguration(cacheIdentifier);


        return configuration.getNamespace();
    }
}
