package com.dylanvann.fastimage;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;

// We need an AppGlideModule to be present for progress events to work.
@GlideModule
public final class FastImageGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        int diskCacheSizeBytes = 1024 * 1024 * 100; // 100 MB

        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2)
                .build();

        builder.setMemoryCache(new MultipleFolderLruResourceCache(calculator.getMemoryCacheSize()));

        builder.setDiskCache(
                new MultipleFolderInternalCacheDiskCacheFactory(context, diskCacheSizeBytes));
    }


}
