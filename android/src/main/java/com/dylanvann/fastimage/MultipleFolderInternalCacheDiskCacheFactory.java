package com.dylanvann.fastimage;

import android.content.Context;

import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;

import java.io.File;

public class MultipleFolderInternalCacheDiskCacheFactory extends MultipleFolderDiskLruCacheFactory {

    public MultipleFolderInternalCacheDiskCacheFactory(Context context, long diskCacheSize) {
        this(context, "", diskCacheSize);
    }

    public MultipleFolderInternalCacheDiskCacheFactory(final Context context, final String diskCacheName,
                                         long diskCacheSize) {
        super(new DiskLruCacheFactory.CacheDirectoryGetter() {
            @Override
            public File getCacheDirectory() {
                File cacheDirectory = context.getCacheDir();
                if (cacheDirectory == null) {
                    return null;
                }
                if (diskCacheName != null) {
                    return new File(cacheDirectory, diskCacheName);
                }
                return cacheDirectory;
            }
        }, diskCacheSize);
    }
}
