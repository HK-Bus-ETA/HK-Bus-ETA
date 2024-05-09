/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
@Immutable
class DataContainer(
    val dataSheet: DataSheet,
    val mtrBusStopAlias: Map<String, List<String>>,
    val kmbSubsidiary: Map<KMBSubsidiary, List<String>>,
    @ReduceDataOmitted val lrtData: Map<String, StationInfo>? = null,
    @ReduceDataOmitted val mtrData: Map<String, StationInfo>? = null,
    @ReduceDataOmitted val mtrBarrierFreeMapping: StationBarrierFreeMapping? = null,
    val updatedTime: Long = currentTimeMillis()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataContainer) return false

        if (dataSheet != other.dataSheet) return false
        if (mtrBusStopAlias != other.mtrBusStopAlias) return false
        if (kmbSubsidiary != other.kmbSubsidiary) return false
        if (lrtData != other.lrtData) return false
        if (mtrData != other.mtrData) return false
        if (mtrBarrierFreeMapping != other.mtrBarrierFreeMapping) return false
        return updatedTime == other.updatedTime
    }

    override fun hashCode(): Int {
        var result = dataSheet.hashCode()
        result = 31 * result + mtrBusStopAlias.hashCode()
        result = 31 * result + kmbSubsidiary.hashCode()
        result = 31 * result + (lrtData?.hashCode() ?: 0)
        result = 31 * result + (mtrData?.hashCode() ?: 0)
        result = 31 * result + (mtrBarrierFreeMapping?.hashCode() ?: 0)
        result = 31 * result + updatedTime.hashCode()
        return result
    }

}