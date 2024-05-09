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
import com.loohp.hkbuseta.common.utils.dispatcherIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonPrimitive


private fun String.formatHolidaysDate(): String {
    return if (length == 8 && !contains('-')) {
        "${substring(0, 4)}-${substring(4, 6)}-${substring(6, 8)}"
    } else {
        this
    }
}

object HolidaySerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Holiday", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString().formatHolidaysDate())
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeSerializableValue(LocalDate.serializer(), value)
    }
}

object OperatorStopIdPairSerializer : KSerializer<Pair<Operator, String>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor("OperatorStopIdPair", JsonArray.serializer().descriptor)

    override fun deserialize(decoder: Decoder): Pair<Operator, String> {
        return decoder.decodeSerializableValue(JsonArray.serializer()).let {
            Operator.valueOf(it[0].jsonPrimitive.content) to it[1].jsonPrimitive.content
        }
    }

    override fun serialize(encoder: Encoder, value: Pair<Operator, String>) {
        encoder.encodeSerializableValue(JsonArray.serializer(), buildJsonArray {
            add(value.first.name)
            add(value.second)
        })
    }
}

@Serializable
@Immutable
class DataSheet(
    val holidays: List<@Serializable(with = HolidaySerializer::class) LocalDate>,
    val routeList: Map<String, Route>,
    val stopList: Map<String, Stop>,
    val stopMap: Map<String, List<@Serializable(with = OperatorStopIdPairSerializer::class) Pair<Operator, String>>>,
    @ReduceDataOmitted val serviceDayMap: Map<String, List<String>>? = null
) {

    val routeNumberList: Set<String> by lazy { routeList.values.asSequence().map { it.routeNumber }.toSet() }
    @Transient
    val standardSortedRouteKeys: Deferred<List<String>> = CoroutineScope(dispatcherIO).async {
        routeList.entries.asSequence().sortedWith(compareBy(routeComparator) { it.value }).map { it.key }.toList()
    }
    @Transient
    val routeNumberFirstSortedRouteKeys: Deferred<List<String>> = CoroutineScope(dispatcherIO).async {
        routeList.entries.asSequence().sortedWith(compareBy(routeComparatorRouteNumberFirst) { it.value }).map { it.key }.toList()
    }
    @Transient
    val routeKeysByStopId: Deferred<Map<String, Set<String>>> = CoroutineScope(dispatcherIO).async {
        val mapping: MutableMap<String, MutableSet<String>> = mutableMapOf()
        for ((key, route) in routeList) {
            for (stopIds in route.stops.values) {
                for (stopId in stopIds) {
                    mapping.getOrPut(stopId) { HashSet() }.add(key)
                }
            }
        }
        mapping
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataSheet) return false

        if (holidays != other.holidays) return false
        if (routeList != other.routeList) return false
        if (stopList != other.stopList) return false
        if (stopMap != other.stopMap) return false
        return serviceDayMap == other.serviceDayMap
    }

    override fun hashCode(): Int {
        var result = holidays.hashCode()
        result = 31 * result + routeList.hashCode()
        result = 31 * result + stopList.hashCode()
        result = 31 * result + stopMap.hashCode()
        result = 31 * result + (serviceDayMap?.hashCode() ?: 0)
        return result
    }

}
