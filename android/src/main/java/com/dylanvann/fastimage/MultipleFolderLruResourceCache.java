package com.dylanvann.fastimage;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.cache.MemoryCache;

public class MultipleFolderLruResourceCache extends MultipleFolderLruCache<Key, Resource<?>> implements MemoryCache {
    private MemoryCache.ResourceRemovedListener listener;

    /**
     * Constructor for LruResourceCache.
     *
     * @param size The maximum size in bytes the in memory cache can use.
     */
    public MultipleFolderLruResourceCache(long size) {
        super(size);
    }

    @Override
    public void setResourceRemovedListener(@NonNull ResourceRemovedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onItemEvicted(@NonNull Key key, @Nullable Resource<?> item) {
        if (listener != null && item != null) {
            listener.onResourceRemoved(item);
        }
    }

    @Override
    protected int getSize(@Nullable Resource<?> item) {
        if (item == null) {
            return super.getSize(null);
        } else {
            return item.getSize();
        }
    }

    @SuppressLint("InlinedApi")
    @Override
    public void trimMemory(int level) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            // Entering list of cached background apps
            // Evict our entire bitmap cache
            clearMemory();
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            // The app's UI is no longer visible, or app is in the foreground but system is running
            // critically low on memory
            // Evict oldest half of our bitmap cache
            trimToSize(getMaxSize() / 2);
        }
    }
}
