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