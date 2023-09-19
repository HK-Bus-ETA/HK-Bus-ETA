package com.loohp.hkbuseta.branchedlist;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.BinaryOperator;

public class BranchedList<K, V> extends LinkedList<BranchedListEntry<K, V>> {

    private final BinaryOperator<V> conflictResolve;

    public BranchedList() {
        this((a, b) -> a);
    }

    public BranchedList(BinaryOperator<V> conflictResolve) {
        this.conflictResolve = conflictResolve;
    }

    public BranchedList(Collection<? extends BranchedListEntry<K, V>> c) {
        this((a, b) -> a, c);
    }

    public BranchedList(BinaryOperator<V> conflictResolve, Collection<? extends BranchedListEntry<K, V>> c) {
        super(c);
        this.conflictResolve = conflictResolve;
    }

    public boolean add(K key, V value) {
        return add(new BranchedListEntry<>(key, value));
    }

    public int keyIndexOf(K key, int from) {
        for (ListIterator<BranchedListEntry<K, V>> itr = listIterator(from); itr.hasNext();) {
            int i = itr.nextIndex();
            if (Objects.equals(key, itr.next().getKey())) {
                return i;
            }
        }
        return -1;
    }

    public int[] match(List<BranchedListEntry<K, V>> other, int searchFrom) {
        for (ListIterator<BranchedListEntry<K, V>> itr = other.listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            int indexOf = keyIndexOf(itr.next().getKey(), searchFrom);
            if (indexOf >= 0) {
                return new int[] {indexOf, i};
            }
        }
        return null;
    }

    public void merge(List<BranchedListEntry<K, V>> other) {
        merge(other, 0, false);
    }

    public void merge(List<BranchedListEntry<K, V>> other, int searchFrom, boolean addToFrontIfNotFound) {
        if (other.isEmpty()) {
            return;
        }
        if (isEmpty()) {
            addAll(other);
            return;
        }
        int[] match = match(other, searchFrom);
        if (match == null) {
            if (addToFrontIfNotFound) {
                addAll(searchFrom, other);
            } else {
                addAll(other);
            }
            return;
        }
        int selfIndex = match[0];
        int otherIndex = match[1];
        BranchedListEntry<K, V> entry = get(selfIndex);
        set(selfIndex, new BranchedListEntry<>(entry.getKey(), conflictResolve.apply(entry.getValue(), other.get(otherIndex).getValue())));
        addAll(selfIndex, other.subList(0, otherIndex));
        other = other.subList(otherIndex + 1, other.size());
        if (!other.isEmpty()) {
            merge(other, selfIndex + 1, true);
        }
    }

    public List<V> values() {
        return Lists.transform(this, BranchedListEntry::getValue);
    }

}
