package org.visallo.core.cache;

public interface CacheListener {
    void invalidate(String cacheName);

    void invalidate(String cacheName, String key);
}
