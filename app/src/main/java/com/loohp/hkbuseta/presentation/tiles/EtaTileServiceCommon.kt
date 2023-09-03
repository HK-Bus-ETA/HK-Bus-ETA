package com.loohp.hkbuseta.presentation.tiles

import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.text.HtmlCompat
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import com.loohp.hkbuseta.presentation.MainActivity
import com.loohp.hkbuseta.presentation.Registry
import com.loohp.hkbuseta.presentation.Shared
import com.loohp.hkbuseta.presentation.utils.StringUtils
import com.loohp.hkbuseta.presentation.utils.StringUtilsKt
import org.json.JSONObject

class EtaTileServiceCommon {

    companion object {

        const val RESOURCES_VERSION = "0"

        fun noFavouriteRouteStop(favoriteIndex: Int, packageName: String): LayoutElementBuilders.LayoutElement {
            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(0F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(360F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(7F).build()
                                )
                                .setColor(
                                    ColorProp.Builder(android.graphics.Color.DKGRAY).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId("open")
                                        .setOnClick(
                                            ActionBuilders.LaunchAction.Builder()
                                                .setAndroidActivity(
                                                    ActionBuilders.AndroidActivity.Builder()
                                                        .setClassName(MainActivity::class.java.name)
                                                        .setPackageName(packageName)
                                                        .build()
                                                ).build()
                                        ).build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setStart(DimensionBuilders.DpProp.Builder(20F).build())
                                                .setEnd(DimensionBuilders.DpProp.Builder(20F).build())
                                                .build()
                                        )
                                        .build()
                                )
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setFontStyle(
                                            FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(25F).build()
                                                )
                                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                .setColor(
                                                    ColorProp.Builder(Color.Yellow.toArgb()).build()
                                                ).build()
                                        )
                                        .setText(favoriteIndex.toString())
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setText(if (Shared.language == "en") {
                                            "\nYou don't have a selected route stop!\nYou can set the route stop in the app."
                                        } else {
                                            "\n你未有選擇路線巴士站\n您可在應用程式中選取"
                                        })
                                        .build()
                                ).build()
                        ).build()
                ).build()
        }

        fun title(index: Int, stopName: JSONObject, routeNumber: String): LayoutElementBuilders.Text {
            var name = stopName.optString(Shared.language)
            if (Shared.language == "en") {
                name = StringUtils.capitalize(name)
            }
            return LayoutElementBuilders.Text.Builder()
                .setText("[".plus(routeNumber).plus("] ").plus(index).plus(". ").plus(name))
                .setMaxLines(Int.MAX_VALUE)
                .setFontStyle(
                    FontStyle.Builder()
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(if (Shared.language == "en") 13F else 17F).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        fun subTitle(destName: JSONObject): LayoutElementBuilders.Text {
            var name = destName.optString(Shared.language)
            name = if (Shared.language == "en") "To " + StringUtils.capitalize(name) else "往$name"
            return LayoutElementBuilders.Text.Builder()
                .setText(name)
                .setMaxLines(Int.MAX_VALUE)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(12F).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        @OptIn(ProtoLayoutExperimental::class)
        fun etaText(lines: Map<Int, String>, seq: Int, singleLine: Boolean): LayoutElementBuilders.Text {
            val text = StringUtilsKt.toAnnotatedString(
                HtmlCompat.fromHtml(
                    lines.getOrDefault(seq, "-"),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            val color = Color.White.toArgb()

            return LayoutElementBuilders.Text.Builder()
                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE)
                .setMarqueeIterations(-1)
                .setMaxLines(if (singleLine) 1 else Int.MAX_VALUE)
                .setText(text.text)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(if (seq == 1) 15F else (if (Shared.language == "en") 11F else 13F)).build()
                        )
                        .setColor(
                            ColorProp.Builder(color).build()
                        )
                        .build()
                ).build()
        }

        fun buildLayout(favouriteStopRoute: JSONObject, packageName: String): LayoutElementBuilders.LayoutElement {
            val stopName = favouriteStopRoute.optJSONObject("stop").optJSONObject("name")
            val destName = favouriteStopRoute.optJSONObject("route").optJSONObject("dest")

            val stopId = favouriteStopRoute.optString("stopId")
            val co = favouriteStopRoute.optString("co")
            val index = favouriteStopRoute.optInt("index")
            val stop = favouriteStopRoute.optJSONObject("stop")
            val route = favouriteStopRoute.optJSONObject("route")

            val eta = Registry.getEta(stopId, co, index, stop, route)

            val color = when (co) {
                "kmb" -> 0xFFFF4747.toInt()
                "ctb" -> 0xFFFFE15E.toInt()
                "nlb" -> 0xFF9BFFC6.toInt()
                "mtr-bus" -> 0xFFAAD4FF.toInt()
                "gmb" -> 0xFFAAFFAF.toInt()
                else -> android.graphics.Color.DKGRAY
            }

            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(0F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(360F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(7F).build()
                                )
                                .setColor(
                                    ColorProp.Builder(color).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId("open")
                                        .setOnClick(
                                            ActionBuilders.LaunchAction.Builder()
                                                .setAndroidActivity(
                                                    ActionBuilders.AndroidActivity.Builder()
                                                        .setClassName(MainActivity::class.java.name)
                                                        .setPackageName(packageName)
                                                        .build()
                                                ).build()
                                        ).build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setStart(DimensionBuilders.DpProp.Builder(35F).build())
                                                .setEnd(DimensionBuilders.DpProp.Builder(35F).build())
                                                .build()
                                        )
                                        .build()
                                )
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .addContent(
                                    title(index, stopName, route.optString("route"))
                                )
                                .addContent(
                                    subTitle(destName)
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(12F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 1, false)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(7F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 2, true)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(7F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 3, true)
                                ).build()
                        ).build()
                ).build()
        }

    }

}