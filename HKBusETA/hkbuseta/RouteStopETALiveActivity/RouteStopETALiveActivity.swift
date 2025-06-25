//
//  RouteStopETALiveActivity.swift
//  RouteStopETALiveActivity
//
//  Created by LOOHP on 24/6/2025.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct RouteStopETALiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        let routeNumber: String
        let hasEta: Bool
        let eta: [String]
        let destination: String
        let stop: String
        let color: Int64
        let url: String
    }
}

struct RouteStopETALiveActivityWidgetView: View {
    
    var state: RouteStopETALiveActivityAttributes.ContentState
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        VStack {
            HStack {
                VStack(alignment: .leading) {
                    HStack(spacing: 2) {
                        Text(state.routeNumber).font(.title).bold()
                        Text(state.destination).font(.title2)
                    }
                    Text(state.stop).font(.subheadline)
                }
                Spacer()
                if state.hasEta {
                    VStack(alignment: .trailing) {
                        Text(state.eta[0]).bold().font(.title3).multilineTextAlignment(.trailing)
                        if state.eta.count > 1 {
                            Text(state.eta[1]).font(.title3).multilineTextAlignment(.trailing)
                        }
                        if state.eta.count > 2 {
                            Text(state.eta[2]).font(.title3).multilineTextAlignment(.trailing)
                        }
                    }
                } else {
                    Image(systemName: "clock").font(.title2).multilineTextAlignment(.trailing)
                }
            }
        }
        .padding(10)
        .activityBackgroundTint(state.color.asColor().adjustBrightness(percentage: colorScheme == .light ? 1.6 : 0.4))
    }
    
}

struct RouteStopETALiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RouteStopETALiveActivityAttributes.self) { context in
            RouteStopETALiveActivityWidgetView(state: context.state)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.state.routeNumber)
                        .bold()
                        .multilineTextAlignment(.leading)
                        .foregroundStyle(context.state.color.asColor().isLight() ? .black : .white)
                        .padding(1)
                        .background(context.state.color.asColor())
                        .cornerRadius(5)
                        .padding(.horizontal)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    if context.state.hasEta {
                        Text(context.state.eta[0]).bold().padding(.horizontal)
                    } else {
                        Image(systemName: "clock").padding(.horizontal)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(context.state.destination).bold().multilineTextAlignment(.leading)
                            Text(context.state.stop).multilineTextAlignment(.leading)
                        }
                        Spacer()
                        if context.state.hasEta {
                            VStack(alignment: .trailing) {
                                if context.state.eta.count > 1 {
                                    Text(context.state.eta[1]).multilineTextAlignment(.trailing)
                                }
                                if context.state.eta.count > 2 {
                                    Text(context.state.eta[2]).multilineTextAlignment(.trailing)
                                }
                                Spacer()
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            } compactLeading: {
                Text(context.state.routeNumber)
                    .bold()
                    .multilineTextAlignment(.leading)
                    .foregroundStyle(context.state.color.asColor().isLight() ? .black : .white)
                    .padding(1)
                    .background(context.state.color.asColor())
                    .cornerRadius(5)
            } compactTrailing: {
                if context.state.hasEta {
                    Text(context.state.eta[0]).bold().multilineTextAlignment(.trailing)
                } else {
                    Image(systemName: "clock")
                }
            } minimal: {
                HStack(spacing: 2) {
                    Text(context.state.routeNumber)
                        .bold()
                        .multilineTextAlignment(.leading)
                        .foregroundStyle(context.state.color.asColor().isLight() ? .black : .white)
                        .padding(1)
                        .background(context.state.color.asColor())
                        .cornerRadius(5)
                    if context.state.hasEta {
                        Text(context.state.eta[0]).bold().multilineTextAlignment(.trailing)
                    } else {
                        Image(systemName: "clock")
                    }
                }
            }
            .widgetURL(URL(string: context.state.url))
            .keylineTint(context.state.color.asColor())
        }
    }
}
