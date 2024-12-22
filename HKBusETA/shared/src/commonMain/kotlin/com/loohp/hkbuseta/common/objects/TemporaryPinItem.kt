package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.Immutable
import kotlin.random.Random


@Immutable
data class TemporaryPinItem(
    val route: Route,
    val co: Operator,
    val stopIndex: Int
) {
    val id: Int = Random.nextInt()
}