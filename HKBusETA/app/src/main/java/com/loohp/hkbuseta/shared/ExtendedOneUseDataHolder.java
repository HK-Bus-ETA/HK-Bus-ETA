package com.loohp.hkbuseta.shared;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ExtendedOneUseDataHolder {

    private static final Map<String, ExtendedOneUseDataHolder> REGISTRY;

    static {
        Cache<String, ExtendedOneUseDataHolder> registry = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        REGISTRY = registry.asMap();
    }

    public static Builder createNew() {
        return new Builder();
    }

    public static ExtendedOneUseDataHolder poll(String key) {
        return REGISTRY.remove(key);
    }

    private final Map<String, Object> extras;

    private ExtendedOneUseDataHolder(Map<String, Object> extras) {
        this.extras = Collections.unmodifiableMap(extras);
    }

    public Object getExtra(String name) {
        return extras.get(name);
    }

    /** @noinspection unchecked*/
    public <T> T getExtra(String name, Class<T> type) {
        return (T) extras.get(name);
    }

    public boolean hasExtra(String name) {
        return extras.containsKey(name);
    }

    public void clear() {
        extras.clear();
    }

    public static class Builder {

        private final Map<String, Object> extras;

        private Builder() {
            this.extras = new HashMap<>();
        }

        public String buildAndRegisterData() {
            String key = UUID.randomUUID().toString();
            ExtendedOneUseDataHolder holder = new ExtendedOneUseDataHolder(extras);
            REGISTRY.put(key, holder);
            return key;
        }

        public Builder extra(String name, Object object) {
            extras.put(name, object);
            return this;
        }

        public Builder extras(ExtendedOneUseDataHolder other) {
            extras.putAll(other.extras);
            return this;
        }

    }

}
