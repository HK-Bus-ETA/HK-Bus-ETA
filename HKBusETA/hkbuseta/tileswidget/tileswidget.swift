//
//  tileswidget.swift
//  tileswidget
//
//  Created by LOOHP on 13/01/2024.
//

import WidgetKit
import SwiftUI

struct Provider: TimelineProvider {
    
    func placeholder(in context: Context) -> WidgetEntry {
        WidgetEntry()
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetEntry) -> ()) {
        completion(WidgetEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        completion(Timeline(entries: [WidgetEntry()], policy: .never))
    }
    
}

struct WidgetEntry: TimelineEntry {
    let date: Date = Date()
}

struct tileswidgetEntryView : View {
    
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var widgetFamily

    var body: some View {
        HStack {
            switch widgetFamily {
            case .accessoryRectangular:
                Rectangle()
                    .frame(width: 4)
                    .foregroundColor(.red)
                VStack(alignment: .leading) {
                    Text("到站預報資訊方塊")
                    Text("ETA Tiles")
                }
            default:
                Text("ETA")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .widgetURL(URL(string: "hkbuseta://tileswidget"))
    }
}

@main
struct tileswidget: Widget {
    
    let kind: String = "tileswidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            tileswidgetEntryView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
    }
}
