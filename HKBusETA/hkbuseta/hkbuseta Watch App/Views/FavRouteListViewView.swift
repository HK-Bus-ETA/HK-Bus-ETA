//
//  NearbyView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 23/12/2023.
//

import SwiftUI
import shared

struct FavRouteListViewView: View {
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var location: LocationResult?
    
    let skipTimer = Timer.publish(every: 2, on: .main, in: .common).autoconnect()
    @State var showingSkip = false
    
    @State private var usingGps: Bool
    
    @State private var failed: Bool = false
    
    init(data: [String: Any]?) {
        self.usingGps = data?["usingGps"] as! Bool
    }
    
    var body: some View {
        ZStack {
            Text(Shared().language == "en" ? "Locating..." : "正在讀取你的位置...")
                .font(.system(size: 20.scaled()))
                .frame(alignment: .center)
                .multilineTextAlignment(.center)
            if showingSkip {
                Button(action: {
                    failed = true
                }) {
                    Text(Shared().language == "en" ? "Skip sort by distance" : "略過按距離排序").bold()
                }
                .frame(width: 170.scaled(), height: 45.scaled())
                .clipShape(RoundedRectangle(cornerRadius: 25))
                .edgesIgnoringSafeArea(.all)
                .offset(x: 0, y: 70.scaled())
            }
        }
        .onChange(of: locationManager.isLocationFetched) {
            if locationManager.authorizationDenied {
                failed = true
            } else if locationManager.isLocationFetched {
                if locationManager.location == nil {
                    failed = true
                } else {
                    location = locationManager.location!.coordinate.toLocationResult()
                    handleLocation()
                }
            }
        }
        .onChange(of: locationManager.readyForRequest) {
            if location == nil && locationManager.readyForRequest {
                locationManager.requestLocation()
            }
        }
        .onAppear {
            if usingGps {
                if locationManager.readyForRequest {
                    locationManager.requestLocation()
                } else if !locationManager.authorizationDenied {
                    locationManager.requestPermission()
                }
            } else {
                handleLocation()
            }
        }
        .onChange(of: failed) {
            if failed {
                handleLocation()
            }
        }
        .onReceive(skipTimer) { _ in
            if !showingSkip {
                showingSkip = true
            }
        }
    }
    
    func handleLocation() {
        dispatcherIO {
            let loc: Coordinates? = usingGps && location != nil ? location!.location! : nil
            let origin: Coordinates? = Shared().favoriteRouteStops.values.filter { $0.favouriteStopMode.isRequiresLocation }.isEmpty ? nil : loc
            let sortedEntries = Shared().favoriteRouteStops.sorted { $0.key.intValue < $1.key.intValue }
            let routeEntries = sortedEntries.compactMap { (_, fav) in
                let resolvedStop = fav.resolveStop(context: appContext(), originGetter: { origin })
                let stopId = resolvedStop.stopId
                let stop = resolvedStop.stop
                let route = resolvedStop.route
                return RouteSearchResultEntry(routeKey: route.getRouteKey(context: appContext())!, route: route, co: fav.co, stopInfo: StopInfo(stopId: stopId, data: stop, distance: 0.0, co: fav.co), origin: nil, isInterchangeSearch: false)
            }
            let distinctEntries = Array(Set(routeEntries.map { $0.uniqueKey })).compactMap { key in
                routeEntries.first { $0.uniqueKey == key }
            }
            let data = newAppDataConatiner()
            data["result"] = distinctEntries
            data["showEta"] = true
            if loc != nil {
                data["proximitySortOrigin"] = loc!
            }
            data["listType"] = RouteListType.Companion().FAVOURITE
            appContext().appendStack(screen: AppScreen.listRoutes, mutableData: data)
            appContext().popSecondLastStack()
        }
    }
}

#Preview {
    FavRouteListViewView(data: nil)
}
