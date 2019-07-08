package com.dylanvann.fastimage;

public class FastImagePreloaderConfiguration {
    private String namespace;

    public FastImagePreloaderConfiguration(String namespace, int maxCacheAge) {
        this.namespace = namespace;
        this.maxCacheAge = maxCacheAge;
    }

    private int maxCacheAge;

    public FastImagePreloaderConfiguration() {

    }


    public String getNamespace() {
        return namespace;
    }

    public int getMaxCacheAge() {
        return maxCacheAge;
    }
}
