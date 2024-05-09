//
//  FavouriteRoutesWidgetExtension.swift
//  FavouriteRoutesWidgetExtension
//
//  Created by LOOHP on 18/4/2024.
//

import WidgetKit
import SwiftUI

struct Provider: AppIntentTimelineProvider {
    
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(configuration: ConfigurationAppIntent())
    }

    func snapshot(for configuration: ConfigurationAppIntent, in context: Context) async -> SimpleEntry {
        SimpleEntry(configuration: configuration)
    }
    
    func timeline(for configuration: ConfigurationAppIntent, in context: Context) async -> Timeline<SimpleEntry> {
        Timeline(entries: [SimpleEntry(configuration: configuration)], policy: .never)
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date = Date()
    let configuration: ConfigurationAppIntent
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
                            Text(route.dest)
                                .font(.system(size: 17.dynamicSize(), weight: .bold))
                                .minimumScaleFactor(0.1)
                        }
                        if let coSpecialRemark = route.coSpecialRemark {
                            Text(coSpecialRemark)
                                .font(.system(size: 15.dynamicSize()))
                                .minimumScaleFactor(0.1)
                                .multilineTextAlignment(.leading)
                        }
                        if let secondLine = route.secondLine {
                            Text(secondLine)
                                .font(.system(size: 15.dynamicSize()))
                                .minimumScaleFactor(0.1)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .widgetURL(URL(string: route.deeplink))
                case .systemSmall:
                    let colors = route.coDisplay.map { $0.color }.filter { $0 != nil }.map { $0!.asColor() }
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .lastTextBaseline, spacing: 0) {
                            ForEach(route.coDisplay.indices, id: \.self) { index in
                                let part = route.coDisplay[index]
                                Text(part.string)
                                    .font(.system(size: 23.dynamicSize()))
                                    .minimumScaleFactor(0.5)
                                    .foregroundColor(part.color?.asColor().adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                            }
                            Spacer(minLength: 3)
                            Image("icon_max")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 25, height: 25, alignment: .topTrailing)
                        }
                        .frame(maxWidth: .infinity)
                        Spacer(minLength: 0)
                        VStack(alignment: .leading, spacing: 0) {
                            Text(route.routeNumber)
                                .font(.system(size: 96.dynamicSize(), weight: .bold))
                                .minimumScaleFactor(0.25)
                            HStack(alignment: .firstTextBaseline, spacing: 0) {
                                if let prependTo = route.prependTo {
                                    Text(prependTo)
                                        .font(.system(size: 17.dynamicSize()))
                                        .minimumScaleFactor(0.25)
                                }
                                Text(route.dest)
                                    .font(.system(size: 23.dynamicSize(), weight: .bold))
                                    .minimumScaleFactor(0.25)
                            }
                            
                        }
                        Spacer(minLength: 3)
                        if let coSpecialRemark = route.coSpecialRemark {
                            Text(coSpecialRemark)
                                .foregroundColor(colors.first!.adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                                .font(.system(size: 17.dynamicSize()))
                                .minimumScaleFactor(0.25)
                                .multilineTextAlignment(.leading)
                                .lineLimit(2)
                        }
                        if let secondLine = route.secondLine {
                            Text(secondLine)
                                .font(.system(size: 17.dynamicSize()))
                                .minimumScaleFactor(0.25)
                                .multilineTextAlignment(.leading)
                                .lineLimit(2)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .containerBackground(LinearGradient(gradient: Gradient(colors: colors.map { $0.adjustBrightness(percentage: colorScheme == .light ? 1.6 : 0.4) }), startPoint: .leading, endPoint: .trailing), for: .widget)
                    .widgetURL(URL(string: route.deeplink))
                default:
                    let colors = route.coDisplay.map { $0.color }.filter { $0 != nil }.map { $0!.asColor() }
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .lastTextBaseline, spacing: 0) {
                            ForEach(route.coDisplay.indices, id: \.self) { index in
                                let part = route.coDisplay[index]
                                Text(part.string)
                                    .font(.system(size: 23.dynamicSize()))
                                    .foregroundColor(part.color?.asColor().adjustBrightness(percentage: colorScheme == .light ? 0.7 : 1.0))
                            }
                            Spacer(minLength: 0)
                            Image("icon_max")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 25, height: 25, alignment: .topTrailing)
                        }
                        .frame(maxWidth: .infinity)
                        Spacer(minLength: 0)
                        if route.co == "mtr" {
                            VStack(alignment: .leading, spacing: 0) {
                                Text(route.routeNumber)
                                    .font(.system(size: 96.dynamicSize(), weight: .bold))
                                    .minimumScaleFactor(0.1)
                                HStack(alignment: .firstTextBaseline, spacing: 0) {
                                    if let prependTo = route.prependTo {
                                        Text(prependTo).font(.system(size: 17.dynamicSize()))
                                    }
                                    Text(route.dest)
                                        .font(.system(size: 23.dynamicSize(), weight: .bold))
                                        .minimumScaleFactor(0.1)
                                }
                                .padding(.bottom, 3)
                            }
                        } else {
                            HStack(alignment: .lastTextBaseline, spacing: 0) {
                                Text(route.routeNumber)
                                    .font(.system(size: 96.dynamicSize(), weight: .bold))
                                    .minimumScaleFactor(0.1)
                                Text(" ").font(.system(size: 23.dynamicSize(), weight: .bold))
                                HStack(alignment: .firstTextBaseline, spacing: 0) {
                                    if let prependTo = route.prependTo {
                                        Text(prependTo).font(.system(size: 17.dynamicSize()))
                                    }
                                    Text(route.dest)
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
                        if let secondLine = route.secondLine {
                            Text(secondLine)
                                .font(.system(size: 17.dynamicSize()))
                                .minimumScaleFactor(0.1)
                                .multilineTextAlignment(.leading)
                                .lineLimit(widgetFamily == .systemMedium ? 2 : nil)
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
