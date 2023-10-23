/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.branchedlist;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BinaryOperator;

import kotlin.Pair;

public class BranchedList<K, V> extends LinkedList<BranchedListEntry<K, V>> {

    private static final Random BRANCH_ID_GENERATOR = new Random();

    private final BinaryOperator<V> conflictResolve;
    private final int branchId;

    public BranchedList() {
        this((a, b) -> a);
    }

    public BranchedList(BinaryOperator<V> conflictResolve) {
        this.conflictResolve = conflictResolve;
        this.branchId = BRANCH_ID_GENERATOR.nextInt();
    }

    public BranchedList(Collection<? extends BranchedListEntry<K, V>> c) {
        this((a, b) -> a, c);
    }

    public BranchedList(BinaryOperator<V> conflictResolve, Collection<? extends BranchedListEntry<K, V>> c) {
        super(c);
        this.conflictResolve = conflictResolve;
        this.branchId = BRANCH_ID_GENERATOR.nextInt();
    }

    private BranchedList(BinaryOperator<V> conflictResolve, Collection<? extends BranchedListEntry<K, V>> c, int branchId) {
        super(c);
        this.conflictResolve = conflictResolve;
        this.branchId = branchId;
    }

    public boolean add(K key, V value) {
        return add(new BranchedListEntry<>(key, value, branchId));
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

    public int[] match(BranchedList<K, V> other, int searchFrom) {
        for (ListIterator<BranchedListEntry<K, V>> itr = other.listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            int indexOf = keyIndexOf(itr.next().getKey(), searchFrom);
            if (indexOf >= 0) {
                return new int[] {indexOf, i};
            }
        }
        return null;
    }

    public BranchedList<K, V> subList(int fromIndex, int toIndex) {
        return new BranchedList<>(conflictResolve, super.subList(fromIndex, toIndex), branchId);
    }

    public void merge(BranchedList<K, V> other) {
        merge(other, 0, false);
    }

    public void merge(BranchedList<K, V> other, int searchFrom, boolean addToFrontIfNotFound) {
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
        set(selfIndex, entry.merge(conflictResolve.apply(entry.getValue(), other.get(otherIndex).getValue()), other.branchId));
        addAll(selfIndex, other.subList(0, otherIndex));
        other = other.subList(otherIndex + 1, other.size());
        if (!other.isEmpty()) {
            merge(other, selfIndex + 1, true);
        }
    }

    public List<V> values() {
        return Lists.transform(this, BranchedListEntry::getValue);
    }

    /** @noinspection StaticPseudoFunctionalStyleMethod*/
    public List<Pair<V, Set<Integer>>> valuesWithBranchIds() {
        return Lists.transform(this, e -> new Pair<>(e.getValue(), e.getBranchIds()));
    }

}
