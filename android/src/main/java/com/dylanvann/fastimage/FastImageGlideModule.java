package com.dylanvann.fastimage;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;

// We need an AppGlideModule to be present for progress events to work.
@GlideModule
public final class FastImageGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setLogLevel(Log.DEBUG);

        FastImageUrlSignatureGenerator.createGenerator(context);

        int diskCacheSizeBytes = 1024 * 1024 * 200; // 200 MB

        builder.setDiskCache(
                new MultipleFolderInternalCacheDiskCacheFactory(context, diskCacheSizeBytes));
    }
}
