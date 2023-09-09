package com.loohp.hkbuseta.presentation.branchedlist;

public class BranchedListEntry<K, V> {

    private final K key;
    private final V value;

    public BranchedListEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}