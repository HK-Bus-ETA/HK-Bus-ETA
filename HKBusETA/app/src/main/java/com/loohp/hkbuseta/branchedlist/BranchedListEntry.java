package com.loohp.hkbuseta.branchedlist;

import com.loohp.hkbuseta.utils.IntUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BranchedListEntry<K, V> {

    private final K key;
    private final V value;
    private final Set<Integer> branchIds;

    public BranchedListEntry(K key, V value, int... branchIds) {
        this(key, value, IntUtils.toList(branchIds));
    }

    public BranchedListEntry(K key, V value, Collection<Integer> branchIds) {
        this.key = key;
        this.value = value;
        this.branchIds = Set.copyOf(branchIds);
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public Set<Integer> getBranchIds() {
        return branchIds;
    }

    public BranchedListEntry<K, V> merge(V value, int... branchIds) {
        Set<Integer> ids = new HashSet<>(this.branchIds);
        ids.addAll(IntUtils.toList(branchIds));
        return new BranchedListEntry<>(key, value, ids);
    }
}