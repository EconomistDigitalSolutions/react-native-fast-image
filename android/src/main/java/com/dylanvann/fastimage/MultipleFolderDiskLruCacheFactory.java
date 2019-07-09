package com.dylanvann.fastimage;

import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;

import java.io.File;

public class MultipleFolderDiskLruCacheFactory implements DiskCache.Factory {


    private final long diskCacheSize;
    private final DiskLruCacheFactory.CacheDirectoryGetter cacheDirectoryGetter;


    @SuppressWarnings("WeakerAccess")
    public MultipleFolderDiskLruCacheFactory(DiskLruCacheFactory.CacheDirectoryGetter cacheDirectoryGetter, long diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
        this.cacheDirectoryGetter = cacheDirectoryGetter;
    }

    @Override
    public DiskCache build() {
        File cacheDir = cacheDirectoryGetter.getCacheDirectory();

        if (cacheDir == null) {
            return null;
        }

        if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory())) {
            return null;
        }

        return new MultiFolderDiskLruCacheWrapper(cacheDir, diskCacheSize);
    }
}
