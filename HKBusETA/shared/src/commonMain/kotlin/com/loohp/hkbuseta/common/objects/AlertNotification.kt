package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.shared.Shared
import kotlinx.serialization.Serializable


@Serializable
data class AlertNotification(
    val id: Int,
    val routes: List<ANRouteIdUrl>,
    val title: BilingualText,
    val content: BilingualText,
    val fallbackUrl: BilingualText,
)

@Serializable
data class ANRouteId(
    val routeNumber: String,
    val operator: List<Operator>
)

@Serializable
data class ANRouteIdUrl(
    val id: ANRouteId,
    val url: BilingualText
)

fun ANRouteId.matches(route: Route): Boolean {
    return route.routeNumber == routeNumber && route.co.any { operator.contains(it) }
}

fun AlertNotification.isInterested(): Boolean {
   return routes.isEmpty() || Shared.favoriteRouteStops.value.any { g -> g.favouriteRouteStops.any { r -> routes.any { i -> i.id.matches(r.route) } } }
}

val AlertNotification.url: BilingualText get() {
    for (group in Shared.favoriteRouteStops.value) {
        for (routeStop in group.favouriteRouteStops) {
            for (route in routes) {
                if (route.id.matches(routeStop.route)) {
                    return route.url
                }
            }
        }
    }
    return fallbackUrl
}

fun AlertNotification.url(): Boolean {
    return routes.isEmpty() || Shared.favoriteRouteStops.value.any { g -> g.favouriteRouteStops.any { r -> routes.any { i -> i.id.matches(r.route) } } }
}