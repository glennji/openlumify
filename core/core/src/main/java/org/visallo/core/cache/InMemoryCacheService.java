package org.visallo.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class InMemoryCacheService implements CacheService {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Map<String, Cache> caches = new HashMap<>();
    private final Set<CacheListener> cacheListeners = new LinkedHashSet<>();

    @Override
    public <T> T put(String cacheName, String key, T t, CacheOptions cacheOptions) {
        return write(() -> {
            Cache<String, T> cache = getOrCreateCache(cacheName, cacheOptions);
            cache.put(key, t);
            return t;
        });
    }

    @Override
    public <T> T getIfPresent(String cacheName, String key) {
        return read(() -> {
            Cache<String, T> cache = getCache(cacheName);
            if (cache == null) {
                return null;
            }
            return cache.getIfPresent(key);
        });
    }

    @Override
    public void invalidate(String cacheName) {
        write(() -> {
            Cache<String, ?> cache = getCache(cacheName);
            if (cache == null) {
                return null;
            }
            cache.invalidateAll();
            cacheListeners.forEach(cacheListener -> cacheListener.invalidate(cacheName));
            return null;
        });
    }

    @Override
    public void invalidate(String cacheName, String key) {
        write(() -> {
            Cache<String, ?> cache = getCache(cacheName);
            if (cache == null) {
                return null;
            }
            cache.invalidate(key);
            cacheListeners.forEach(cacheListener -> cacheListener.invalidate(cacheName, key));
            return null;
        });
    }

    @Override
    public void register(CacheListener cacheListener) {
        this.cacheListeners.add(cacheListener);
    }

    private <T> Cache<String, T> getCache(String cacheName) {
        //noinspection unchecked
        return caches.get(cacheName);
    }

    private <T> Cache<String, T> getOrCreateCache(String cacheName, CacheOptions cacheOptions) {
        Cache<String, T> cache = getCache(cacheName);
        if (cache != null) {
            return cache;
        }
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if (cacheOptions.getMaximumSize() != null) {
            builder.maximumSize(cacheOptions.getMaximumSize());
        }
        cache = builder.build();
        caches.put(cacheName, cache);
        return cache;
    }

    private <T> T write(Provider<T> provider) {
        readWriteLock.writeLock().lock();
        try {
            return provider.get();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private <T> T read(Provider<T> provider) {
        readWriteLock.readLock().lock();
        try {
            return provider.get();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
