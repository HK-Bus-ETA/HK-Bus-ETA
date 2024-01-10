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
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.usingGps = data["usingGps"] as! Bool
    }
    
    var body: some View {
        ZStack {
            Text(Shared().language == "en" ? "Locating..." : "正在讀取你的位置...")
                .font(.system(size: 20.scaled(appContext)))
                .frame(alignment: .center)
                .multilineTextAlignment(.center)
            if showingSkip {
                Button(action: {
                    failed = true
                }) {
                    Text(Shared().language == "en" ? "Skip sort by distance" : "略過按距離排序")
                        .font(.system(size: 17.scaled(appContext), weight: .bold))
                }
                .frame(width: 170.scaled(appContext), height: 45.scaled(appContext))
                .clipShape(RoundedRectangle(cornerRadius: 25))
                .edgesIgnoringSafeArea(.all)
                .offset(x: 0, y: 70.scaled(appContext))
            }
        }
        .onChange(of: locationManager.isLocationFetched) { _ in
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
        .onChange(of: locationManager.readyForRequest) { _ in
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
        .onChange(of: failed) { _ in
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
            let data = newAppDataConatiner()
            data["result"] = Shared().sortedForListRouteView(instance: appContext, origin: origin)
            data["showEta"] = true
            if loc != nil {
                data["proximitySortOrigin"] = loc!
            }
            data["listType"] = RouteListType.Companion().FAVOURITE
            data["allowAmbient"] = true
            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listRoutes, data))
            appContext.finishAffinity()
        }
    }
}
