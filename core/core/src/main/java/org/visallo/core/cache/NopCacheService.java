package org.visallo.core.cache;

import java.util.LinkedHashSet;
import java.util.Set;

public class NopCacheService implements CacheService {
    private final Set<CacheListener> cacheListeners = new LinkedHashSet<>();

    @Override
    public <T> T put(String cacheName, String key, T t, CacheOptions cacheOptions) {
        return t;
    }

    @Override
    public <T> T getIfPresent(String cacheName, String key) {
        return null;
    }

    @Override
    public void invalidate(String cacheName) {
        cacheListeners.forEach(cacheListener -> cacheListener.invalidate(cacheName));
    }

    @Override
    public void invalidate(String cacheName, String key) {
        cacheListeners.forEach(cacheListener -> cacheListener.invalidate(cacheName, key));
    }

    @Override
    public void register(CacheListener cacheListener) {
        this.cacheListeners.add(cacheListener);
    }
}
