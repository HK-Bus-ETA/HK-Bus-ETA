package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.Immutable


@Immutable
class TemporaryPinItem private constructor(
    val key: String,
    val routeNumber: String,
    val bound: String,
    val co: Operator,
    val gmbRegion: GMBRegion?,
    val stopIndex: Int,
    val branches: List<Route>
) {
    constructor(
        route: Route,
        co: Operator,
        stopIndex: Int,
        branches: List<Route>
    ): this(
        key = "${route.routeGroupKey(co)},$stopIndex",
        routeNumber = route.routeNumber,
        bound = route.idBound(co),
        co = co,
        gmbRegion = route.gmbRegion,
        stopIndex = stopIndex,
        branches = branches
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemporaryPinItem) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}