package com.loohp.hkbuseta.presentation.branchedlist;

public class BranchListEntry<K, V> {

    private final K key;
    private final V value;

    public BranchListEntry(K key, V value) {
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