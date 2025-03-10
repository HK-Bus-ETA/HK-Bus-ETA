//
//  NearbyView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 23/12/2023.
//

import SwiftUI
import shared

struct NearbyView: AppScreenView {
    
    @ObservedObject private var locationManager = SingleLocationManager()
    
    @State private var location: LocationResult?
    @State private var exclude: [String]
    @State private var interchangeSearch: Bool
    
    @State private var closestStop: Stop? = nil
    @State private var closesDistance: Double? = nil
    
    @State private var denied = false
    @State private var noNearby: Bool = false
    @State private var failed: Bool = false
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        let lat = data["lat"] as? Double
        let lng = data["lng"] as? Double
        if lat != nil && lng != nil {
            self.location = LocationResult.Companion().of(lat: lat!, lng: lng!, altitude: nil, bearing: nil)
        } else {
            self.location = nil
        }
        self.exclude = data["exclude"] as? [String] ?? []
        self.interchangeSearch = data["interchangeSearch"] as? Bool ?? false
    }
    
    var body: some View {
        VStack(alignment: .center, spacing: 5.scaled(appContext)) {
            if noNearby {
                Text(Shared().language == "en" ? "There are no nearby bus stops" : "附近沒有巴士站")
                    .font(.system(size: 20.scaled(appContext, true)))
                    .frame(alignment: .center)
                    .multilineTextAlignment(.center)
                Text(Shared().language == "en" ? ("Nearest Stop: \(closestStop!.name.en) (\(Int(closesDistance! * 1000).formattedWithDecimalSeparator())m)") : ("最近的巴士站: \(closestStop!.name.zh) (\(Int(closesDistance! * 1000).formattedWithDecimalSeparator())米)"))
                    .font(.system(size: 13.scaled(appContext, true)))
                    .frame(alignment: .center)
                    .multilineTextAlignment(.center)
            } else if failed {
                Text(Shared().language == "en" ? "Unable to read your location" : "無法讀取你的位置")
                    .font(.system(size: 20.scaled(appContext, true)))
                    .frame(alignment: .center)
                    .multilineTextAlignment(.center)
                Text(Shared().language == "en" ? "Please check whether your GPS is enabled" : "請檢查你的定位服務是否已開啟")
                    .font(.system(size: 15.scaled(appContext, true)))
                    .frame(alignment: .center)
                    .multilineTextAlignment(.center)
            } else {
                if location == nil {
                    Text(Shared().language == "en" ? "Locating..." : "正在讀取你的位置...")
                        .font(.system(size: min(20.scaled(appContext, true), 23.scaled(appContext))))
                        .frame(alignment: .center)
                        .multilineTextAlignment(.center)
                } else {
                    Text(Shared().language == "en" ? "Searching Nearby..." : "正在搜尋附近路線...")
                        .font(.system(size: min(20.scaled(appContext, true), 23.scaled(appContext))))
                        .frame(alignment: .center)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .onChange(of: locationManager.isLocationFetched) { _ in
            if locationManager.authorizationDenied {
                denied = true
            } else if locationManager.isLocationFetched {
                if locationManager.location == nil {
                    failed = true
                } else {
                    location = locationManager.location!.toLocationResult()
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
            if location == nil {
                if locationManager.readyForRequest {
                    locationManager.requestLocation()
                } else if !locationManager.authorizationDenied {
                    locationManager.requestPermission()
                }
            } else {
                handleLocation()
            }
        }
        .onChange(of: denied) { denied in
            appContext.showToastText(text: Shared().language == "en" ? "Location Access Permission Denied" : "位置存取權限被拒絕", duration: ToastDuration.short_)
        }
    }
    
    func handleLocation() {
        dispatcherIO {
            if location != nil {
                let loc = location!.location!
                let excludedRoutes = {
                    var map: [Operator: Set<String>] = [:]
                    for entry in exclude {
                        let parts = entry.components(separatedBy: ",")
                        let co = Operator.Companion().valueOf(name: parts[0])
                        let routeNumber = parts[1]
                        if map[co] == nil {
                            map[co] = Set()
                        }
                        map[co]!.insert(routeNumber)
                    }
                    return map
                }()
                let result: Registry.NearbyRoutesResult? = registry(appContext).getNearbyRoutesBlocking(origin: loc, excludedRoutes: excludedRoutes, isInterchangeSearch: interchangeSearch)
                if result == nil {
                    failed = true
                } else {
                    let list = result!.result
                    if list.isEmpty {
                        closestStop = result!.closestStop
                        closesDistance = result!.closestDistance
                        noNearby = true
                    } else {
                        let data = newAppDataConatiner()
                        data["result"] = list
                        data["showEta"] = true
                        data["recentSort"] = RecentSortMode.choice
                        data["proximitySortOrigin"] = Coordinates(lat: result!.origin.lat, lng: result!.origin.lng)
                        data["listType"] = RouteListType.Companion().NEARBY
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listRoutes, data))
                        appContext.finish()
                    }
                }
            }
        }
    }
}
