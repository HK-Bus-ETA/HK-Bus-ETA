//
//  ListRouteView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared
import KMPNativeCoroutinesCore
import KMPNativeCoroutinesRxSwift
import KMPNativeCoroutinesAsync
import KMPNativeCoroutinesCombine
import RxSwift

struct ListRouteView: View {
    
    @State var result: [StopIndexedRouteSearchResultEntry]
    @State var listType: RouteListType
    @State var showEta: Bool
    @State var recentSort: RecentSortMode
    @State var proximitySortOrigin: Coordinates?
    @State var allowAmbient: Bool
    
    init(data: [String: Any]?) {
        let rawResult = data!["result"]! as! [Any]
        var casedResult: [StopIndexedRouteSearchResultEntry]
        if rawResult.isEmpty {
            casedResult = []
        } else if let _ = rawResult.first as? StopIndexedRouteSearchResultEntry {
            casedResult = rawResult as! [StopIndexedRouteSearchResultEntry]
        } else {
            let a = rawResult as! [RouteSearchResultEntry]
            casedResult = a.map { StopIndexedRouteSearchResultEntry.Companion().fromRouteSearchResultEntry(resultEntry: $0) }
        }
        casedResult.removeAll(where: {
            if $0.route == nil {
                let route = registry().findRouteByKey(lookupKey: $0.routeKey, routeNumber: nil)
                if route == nil {
                    return true
                } else {
                    $0.route = route
                }
            }
            if $0.stopInfo != nil && $0.stopInfo!.data == nil {
                let stop = registry().getStopById(stopId: $0.stopInfo!.stopId)
                if stop == nil {
                    return true
                } else {
                    $0.stopInfo!.data = stop
                }
            }
            return false
        })
        casedResult.forEach {
            let route = $0.route
            let co = $0.co
            let stopInfo = $0.stopInfo
            if route != nil && stopInfo != nil {
                $0.stopInfoIndex = Int32(truncatingIfNeeded: registry().getAllStops(routeNumber: route!.routeNumber, bound: route!.bound[co]!, co: co, gmbRegion: route!.gmbRegion).firstIndex(where: {
                    $0.stopId == stopInfo?.stopId
                })!)
            }
        }
        self.result = casedResult
        self.listType = data?["listType"] as? RouteListType ?? RouteListType.Companion().NORMAL
        self.showEta = data?["showEta"] as? Bool ?? false
        self.recentSort = data?["recentSort"] as? RecentSortMode ?? RecentSortMode.disabled
        self.proximitySortOrigin = data?["proximitySortOrigin"] as? Coordinates
        self.allowAmbient = data?["allowAmbient"] as? Bool ?? false
    }
    
    var body: some View {
        ZStack {
            VStack {
                BackButton { $0.screen == AppScreen.listRoutes }
                Text(result.count.description)
            }
        }
    }
}

#Preview {
    ListRouteView(data: nil)
}
