package com.loohp.hkbuseta.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.screenGroup
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.StopInfo
import com.loohp.hkbuseta.common.objects.getRouteKey
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentMinuteState
import com.loohp.hkbuseta.compose.Close
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.RightToLeftRow
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.applyIfNotNull
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformTopBarColor
import com.loohp.hkbuseta.compose.verticalScrollBar
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.renderedSize
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryPinInterface(
    instance: AppActiveContext,
    onSizeChange: ((IntSize) -> Unit)? = null
) {
    var items by Shared.pinnedItems.collectAsStateMultiplatform()

    val window = currentLocalWindowSize
    val now by currentMinuteState.collectAsStateMultiplatform()

    val scroll = rememberLazyListState()
    val routeNumberWidth = if (Shared.language == "en") "249M".renderedSize(30F.sp) else "機場快線".renderedSize(22F.sp)

    val etaResults: MutableMap<String, Registry.ETAQueryResult> = remember { ConcurrentMap() }
    val etaUpdateTimes: MutableMap<String, Long> = remember { ConcurrentMap() }

    val etaResultsState = remember { etaResults.asImmutableState() }
    val etaUpdateTimesState = remember { etaUpdateTimes.asImmutableState() }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeightIn(max = (window.height / 4F).equivalentDp)
            .verticalScrollBar(
                state = scroll,
                scrollbarConfig = ScrollBarConfig(
                    indicatorThickness = 6.dp,
                    padding = PaddingValues(0.dp, 2.dp, 0.dp, 2.dp)
                )
            )
            .background(platformTopBarColor)
            .animateContentSize()
            .applyIfNotNull(onSizeChange) { this.onSizeChanged(it) },
        state = scroll
    ) {
        items(items, key = { it.key }) { item ->
            val allStops by remember(item) { derivedStateOf { Registry.getInstance(instance).getAllStops(item.routeNumber, item.bound, item.co, item.gmbRegion) } }
            val stopData by remember { derivedStateOf { allStops[item.stopIndex - 1] } }
            val branchByActiveness by remember(item) { derivedStateOf { item.branches.currentBranchStatus(now, instance).asSequence().sortedByDescending { it.value.activeness }.map { it.key }.toList() } }

            val route by remember { derivedStateOf { branchByActiveness.first { stopData.branchIds.contains(it) } } }
            val routeKey by remember(item) { derivedStateOf { route.getRouteKey(instance)!! } }

            val primaryColor by remember(item) { derivedStateOf { item.co.getLineColor(route.routeNumber, Color.Red) } }
            val secondaryColor by remember(item) { derivedStateOf { if (route.isKmbCtbJoint) Operator.CTB.getLineColor(route.routeNumber, Color.Red) else primaryColor } }
            val routeEntry by remember(item) { derivedStateOf {
                StopIndexedRouteSearchResultEntry(
                    routeKey = route.getRouteKey(instance)!!,
                    route = route,
                    co = item.co,
                    stopInfo = StopInfo(
                        stopId = stopData.stopId,
                        data = stopData.stop,
                        distance = 0.0,
                        co = item.co,
                        stopIndex = item.stopIndex
                    ),
                    stopInfoIndex = item.stopIndex,
                    origin = null,
                    isInterchangeSearch = false,
                    favouriteStopMode = null,
                    cachedAllStops = allStops.asImmutableList(),
                )
            } }

            RightToLeftRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            brush = Brush.verticalGradient(
                                0F to primaryColor,
                                1F to secondaryColor
                            ),
                            strokeWidth = 4.dp.toPx(),
                            start = Offset(8.dp.toPx(), 7.dp.toPx()),
                            end = Offset(8.dp.toPx(), size.height - 7.dp.toPx())
                        )
                    }
                    .combinedClickable(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                Registry.getInstance(instance).addLastLookupRoute(routeKey, instance)
                                val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                                intent.putExtra("route", routeEntry)
                                if (routeEntry.stopInfo != null) {
                                    intent.putExtra("stopId", routeEntry.stopInfo!!.stopId)
                                }
                                intent.putExtra("stopIndex", routeEntry.stopInfoIndex)
                                if (HistoryStack.historyStack.value.last().screenGroup == AppScreenGroup.ROUTE_STOPS) {
                                    instance.startActivity(AppIntent(instance, AppScreen.DUMMY))
                                    delay(300)
                                }
                                instance.startActivity(intent)
                            }
                        },
                    )
                    .animateItem()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformButton(
                    modifier = Modifier
                        .size(32F.dp)
                        .clip(CircleShape)
                        .plainTooltip(if (Shared.language == "en") "Remove From Pinned" else "取消置頂"),
                    colors = ButtonDefaults.clearColors(),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        items = items.toMutableList().apply { removeAll { it.key == item.key } }
                    }
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(27F.dp),
                        painter = PlatformIcons.Filled.Close,
                        tint = Color.Red,
                        contentDescription = if (Shared.language == "en") "Remove From Pinned" else "取消置頂"
                    )
                }
                RouteRow(
                    key = route.getRouteKey(instance)!!,
                    listType = RouteListType.NORMAL,
                    routeNumberWidth = routeNumberWidth.size.width,
                    showEta = true,
                    deleteFunction = null,
                    route = routeEntry,
                    checkSpecialDest = true,
                    etaResults = etaResultsState,
                    etaUpdateTimes = etaUpdateTimesState,
                    simpleStyle = true,
                    instance = instance
                )
            }
            HorizontalDivider()
        }
    }
    if (items.isNotEmpty() && (scroll.canScrollForward || scroll.canScrollBackward)) {
        HorizontalDivider()
    }
}