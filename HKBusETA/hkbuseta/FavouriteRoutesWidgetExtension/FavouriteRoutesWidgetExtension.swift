//
//  FavouriteRoutesWidgetExtension.swift
//  FavouriteRoutesWidgetExtension
//
//  Created by LOOHP on 18/4/2024.
//

import WidgetKit
import SwiftUI
import shared

struct Provider: AppIntentTimelineProvider {
    
    func placeholder(in context: Context) -> SimpleEntry {
        defer { restrictKotlinHeap() }
        let extendedData = produceExtendedWidgetData(nil, true)
        return SimpleEntry(configuration: ConfigurationAppIntent(), extendedData: extendedData)
    }

    func snapshot(for configuration: ConfigurationAppIntent, in context: Context) async -> SimpleEntry {
        defer { restrictKotlinHeap() }
        let extendedData = produceExtendedWidgetData(configuration, context.isPreview)
        return SimpleEntry(configuration: configuration, extendedData: extendedData)
    }
    
    func timeline(for configuration: ConfigurationAppIntent, in context: Context) async -> Timeline<SimpleEntry> {
        defer { restrictKotlinHeap() }
        let extendedData = produceExtendedWidgetData(configuration, context.isPreview)
        let firstService = extendedData?.getEtaLines(size: 1).first?.date?.toLocalDate()
        let quickestUpdate = .now + 300
        let latestUpdate = .now + 1200
        let nextUpdate = firstService == nil ? quickestUpdate : min(max(quickestUpdate, firstService!), latestUpdate)
        return Timeline(entries: [SimpleEntry(configuration: configuration, extendedData: extendedData)], policy: .after(nextUpdate))
    }
    
    func produceExtendedWidgetData(_ configuration: ConfigurationAppIntent?, _ preview: Bool) -> ExtendedWidgetData? {
        return autoreleasepool {
            restrictKotlinHeap()
            if let route = configuration?.selectedRoute {
                if preview {
                    return WidgetHelperKt.buildPreviewWidgetData(destName: route.dest)
                } else {
                    return WidgetHelperKt.buildWidgetData(precomputedData: route.precomputedData)
                }
            } else {
                return nil
            }
        }
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date = Date()
    let configuration: ConfigurationAppIntent
    let extendedData: ExtendedWidgetData?
}

struct FavouriteRoutesWidgetExtensionEntryView : View {
    
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var widgetFamily
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        if isNewInstall() {
            switch widgetFamily {
            case .systemSmall, .accessoryRectangular:
                VStack(alignment: .center, spacing: 0) {
                    Text("請開啟應用程式")
                        .multilineTextAlignment(.center)
                    Text("Please open the app")
                        .multilineTextAlignment(.center)
                        .padding(.top)
                }
                .containerBackground(.fill.tertiary, for: .widget)
            default:
                VStack(alignment: .center, spacing: 0) {
                    HStack {
                        Image("icon_max")
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 60.0, height: 60.0)
                        Text("香港巴士到站預報\nHK Bus ETA")
                            .multilineTextAlignment(.leading)
                    }
                    Text("請開啟應用程式")
                        .multilineTextAlignment(.center)
                        .padding(.top)
                    Text("Please open the app")
                        .multilineTextAlignment(.center)
                }
                .containerBackground(.fill.tertiary, for: .widget)
            }
        } else {
            if let route = entry.configuration.selectedRoute {
                switch widgetFamily {
                case .accessoryRectangular:
                    let etaLines = entry.extendedData?.getEtaLines(size: 2)
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .lastTextBaseline, spacing: 0) {
                            Text(route.routeNumber)
                                .padding(.trailing, 5.dynamicSize())
                                .font(.system(size: 25.dynamicSize(), weight: .bold))
                                .minimumScaleFactor(0.1)
                            if let prependTo = route.prependTo {
                                Text(prependTo)
                                    .font(.system(size: 13.dynamicSize()))
                                    .minimumScaleFactor(0.1)
                            }
                            Text(entry.extendedData?.resolvedDestName ?? route.dest)
                                .font(.system(size: 17.dynamicSize(), weight: .bold))
                                .minimumScaleFactor(0.1)
                        }
                        if let coSpecialRemark = route.coSpecialRemark {
                            Text(coSpecialRemark)
                                .font(.system(size: 15.dynamicSize()))
                                .minimumScaleFactor(0.1)
                                .multilineTextAlignment(.leading)
                        }
                        let secondLine = route.secondLine ?? ((entry.extendedData?.resolvedStopName ?? "") + (entry.extendedData?.closestStopLabel ?? ""))
                        Text(secondLine)
                            .font(.system(size: 15.dynamicSize()))
                            .minimumScaleFactor(0.1)
                            .multilineTextAlignment(.leading)
                        if let etaLines = etaLines {
                            HStack(alignment: .center, spacing: 5) {
                                ForEach(etaLines.indices, id: \.self) { index in
                                    let etaLine = etaLines[index]
                                    if let date = etaLine.date {
                                        Text(date.toFormattedDate())
                                            .font(.system(size: 20.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    } else {
                                        Text(etaLine.text ?? "")
                                            .font(.system(size: 20.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    }
                                    if index != etaLines.count - 1 {
                                        Spacer(minLength: 0)
                                    }
                                }
                            }
                            .padding(.horizontal, 5)
                            .background {
                                RoundedRectangle(cornerSize: CGSize(width: 10, height: 10))
                                    .fill(colorScheme == .light ? .white : .black)
                                    .frame(maxWidth: .infinity)
                            }
                            .padding(.vertical, 5)
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .widgetURL(URL(string: route.deeplink))
                case .systemSmall:
                    let etaLines = entry.extendedData?.getEtaLines(size: 2)
                    let colors = route.coDisplay.map { $0.color }.filter { $0 != nil }.map { $0!.asColor() }
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .center, spacing: 0) {
                            HStack(alignment: .firstTextBaseline, spacing: 0) {
                                ForEach(route.coDisplay.indices, id: \.self) { index in
                                    let part = route.coDisplay[index]
                                    Text(part.string)
                                        .font(.system(size: 23.dynamicSize()))
                                        .foregroundColor(part.color?.asColor().adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                                }
                            }
                            Spacer(minLength: 0)
                            Image("icon_max")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 25, height: 25)
                        }
                        .frame(maxWidth: .infinity)
                        Spacer(minLength: 0)
                        Text(route.routeNumber)
                            .font(.system(size: 96.dynamicSize(), weight: .bold))
                            .minimumScaleFactor(0.01)
                        HStack(alignment: .firstTextBaseline, spacing: 0) {
                            if let prependTo = route.prependTo {
                                Text(prependTo)
                                    .font(.system(size: 17.dynamicSize()))
                                    .minimumScaleFactor(0.08)
                            }
                            Text(entry.extendedData?.resolvedDestName ?? route.dest)
                                .font(.system(size: 23.dynamicSize(), weight: .bold))
                                .minimumScaleFactor(0.08)
                        }
                        Spacer(minLength: 3)
                        if let coSpecialRemark = route.coSpecialRemark {
                            Text(coSpecialRemark)
                                .foregroundColor(colors.first!.adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                                .font(.system(size: 17.dynamicSize()))
                                .minimumScaleFactor(0.08)
                                .multilineTextAlignment(.leading)
                                .lineLimit(2)
                        }
                        let secondLine = route.secondLine ?? ((entry.extendedData?.resolvedStopName ?? "") + (entry.extendedData?.closestStopLabel ?? ""))
                        Text(secondLine)
                            .font(.system(size: 17.dynamicSize()))
                            .minimumScaleFactor(0.08)
                            .multilineTextAlignment(.leading)
                            .lineLimit(2)
                        if let etaLines = etaLines {
                            Spacer(minLength: 0)
                            HStack(alignment: .center, spacing: 5) {
                                if etaLines.count == 1 {
                                    Spacer(minLength: 0)
                                }
                                ForEach(etaLines.indices, id: \.self) { index in
                                    let etaLine = etaLines[index]
                                    if let date = etaLine.date {
                                        Text(date.toFormattedDate())
                                            .font(.system(size: 20.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    } else {
                                        Text(etaLine.text ?? "")
                                            .font(.system(size: 20.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    }
                                    if index != etaLines.count - 1 {
                                        Spacer(minLength: 0)
                                    }
                                }
                                if etaLines.count == 1 {
                                    Spacer(minLength: 0)
                                }
                            }
                            .padding(.horizontal, 5)
                            .background {
                                RoundedRectangle(cornerSize: CGSize(width: 10, height: 10))
                                    .fill(colorScheme == .light ? .white : .black)
                                    .frame(maxWidth: .infinity)
                            }
                            .padding(.vertical, 5)
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .containerBackground(LinearGradient(gradient: Gradient(colors: colors.map { $0.adjustBrightness(percentage: colorScheme == .light ? 1.6 : 0.4) }), startPoint: .leading, endPoint: .trailing), for: .widget)
                    .widgetURL(URL(string: route.deeplink))
                default:
                    let etaLines = entry.extendedData?.getEtaLines(size: 3)
                    let colors = route.coDisplay.map { $0.color }.filter { $0 != nil }.map { $0!.asColor() }
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .center, spacing: 10) {
                            HStack(alignment: .firstTextBaseline, spacing: 0) {
                                ForEach(route.coDisplay.indices, id: \.self) { index in
                                    let part = route.coDisplay[index]
                                    Text(part.string)
                                        .font(.system(size: 23.dynamicSize()))
                                        .foregroundColor(part.color?.asColor().adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                                }
                            }
                            Spacer(minLength: 0)
                            if let lastUpdatedLabel = entry.extendedData?.lastUpdatedLabel {
                                Text(lastUpdatedLabel)
                                    .truncationMode(.head)
                                    .font(.system(size: 14.dynamicSize()))
                                    .minimumScaleFactor(0.1)
                            }
                            Image("icon_max")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 25, height: 25)
                        }
                        .frame(maxWidth: .infinity)
                        Spacer(minLength: 0)
                        if route.co == "mtr" && widgetFamily != .systemMedium {
                            VStack(alignment: .leading, spacing: 0) {
                                Text(route.routeNumber)
                                    .font(.system(size: 96.dynamicSize(), weight: .bold))
                                    .minimumScaleFactor(0.1)
                                HStack(alignment: .firstTextBaseline, spacing: 0) {
                                    if let prependTo = route.prependTo {
                                        Text(prependTo).font(.system(size: 17.dynamicSize()))
                                    }
                                    Text(entry.extendedData?.resolvedDestName ?? route.dest)
                                        .font(.system(size: 96.dynamicSize(), weight: .bold))
                                        .minimumScaleFactor(0.1)
                                        .lineLimit(1)
                                }
                                .padding(.bottom, 3)
                            }
                        } else {
                            HStack(alignment: .lastTextBaseline, spacing: 0) {
                                Text(route.routeNumber)
                                    .font(.system(size: 72.dynamicSize(), weight: .bold))
                                    .minimumScaleFactor(0.1)
                                    .lineLimit(1)
                                Text(" ").font(.system(size: 23.dynamicSize(), weight: .bold))
                                HStack(alignment: .firstTextBaseline, spacing: 0) {
                                    if let prependTo = route.prependTo {
                                        Text(prependTo).font(.system(size: 17.dynamicSize()))
                                    }
                                    Text(entry.extendedData?.resolvedDestName ?? route.dest)
                                        .font(.system(size: 23.dynamicSize(), weight: .bold))
                                        .minimumScaleFactor(0.1)
                                }
                            }
                        }
                        Spacer(minLength: 0)
                        if let coSpecialRemark = route.coSpecialRemark {
                            Text(coSpecialRemark)
                                .foregroundColor(colors.first!.adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                                .font(.system(size: 17.dynamicSize()))
                                .minimumScaleFactor(0.1)
                                .multilineTextAlignment(.leading)
                                .lineLimit(widgetFamily == .systemMedium ? 2 : nil)
                        }
                        let secondLine = route.secondLine ?? ((entry.extendedData?.resolvedStopName ?? "") + (entry.extendedData?.closestStopLabel ?? ""))
                        Text(secondLine)
                            .font(.system(size: 17.dynamicSize()))
                            .minimumScaleFactor(0.1)
                            .multilineTextAlignment(.leading)
                            .lineLimit(widgetFamily == .systemMedium ? 2 : nil)
                        if let etaLines = etaLines {
                            Spacer(minLength: 0)
                            HStack(alignment: .center, spacing: 10) {
                                if etaLines.count == 1 {
                                    Spacer(minLength: 0)
                                }
                                ForEach(etaLines.indices, id: \.self) { index in
                                    let etaLine = etaLines[index]
                                    if let date = etaLine.date {
                                        Text(date.toFormattedDate())
                                            .font(.system(size: 28.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    } else {
                                        Text(etaLine.text ?? "")
                                            .font(.system(size: 28.dynamicSize(), weight: index == 0 && entry.extendedData?.hasServices == true ? .bold : .regular))
                                            .foregroundColor(.primary.adjustAlpha(percentage: entry.extendedData?.hasServices == true ? 1 : 0.5))
                                            .minimumScaleFactor(0.1)
                                            .lineLimit(1)
                                    }
                                    if index != etaLines.count - 1 {
                                        Spacer(minLength: 0)
                                    }
                                }
                                if etaLines.count == 1 {
                                    Spacer(minLength: 0)
                                }
                            }
                            .padding(.horizontal, 5)
                            .background {
                                RoundedRectangle(cornerSize: CGSize(width: 10, height: 10))
                                    .fill(colorScheme == .light ? .white : .black)
                                    .frame(maxWidth: .infinity)
                            }
                            .padding(.vertical, 5)
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .containerBackground(LinearGradient(gradient: Gradient(colors: colors.map { $0.adjustBrightness(percentage: colorScheme == .light ? 1.6 : 0.4) }), startPoint: .leading, endPoint: .trailing), for: .widget)
                    .widgetURL(URL(string: route.deeplink))
                }
            } else {
                switch widgetFamily {
                case .systemSmall, .accessoryRectangular:
                    VStack(alignment: .center, spacing: 0) {
                        Text("請設置小工具")
                            .multilineTextAlignment(.center)
                        Text("Please configure widget")
                            .multilineTextAlignment(.center)
                            .padding(.top)
                    }
                    .containerBackground(.fill.tertiary, for: .widget)
                default:
                    VStack(alignment: .center, spacing: 0) {
                        HStack {
                            Image("icon_max")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 60.0, height: 60.0)
                            Text("香港巴士到站預報\nHK Bus ETA")
                                .multilineTextAlignment(.leading)
                        }
                        Text("請設置小工具")
                            .multilineTextAlignment(.center)
                            .padding(.top)
                        Text("Please configure widget")
                            .multilineTextAlignment(.center)
                    }
                    .containerBackground(.fill.tertiary, for: .widget)
                }
            }
        }
    }
}

struct FavouriteRoutesWidgetExtension: Widget {
    let kind: String = "FavouriteRoutesWidgetExtension"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: ConfigurationAppIntent.self, provider: Provider()) { entry in
            FavouriteRoutesWidgetExtensionEntryView(entry: entry)
        }
        .configurationDisplayName("widget.name")
        .description("widget.description")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular])
    }
}
