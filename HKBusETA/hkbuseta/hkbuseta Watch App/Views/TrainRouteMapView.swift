//
//  ContentView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

private let mtrRouteMapData: RouteMapData = {
    if let path = Bundle.main.path(forResource: "mtr_system_map", ofType: "json") {
        do {
            return RouteMapData.Companion().fromString(input: try String(contentsOfFile: path, encoding: .utf8))
        } catch {
        }
    }
    return RouteMapData.init(width: 1500, height: 1000, stations: [:])
}()

private let lightRailRouteMapData: RouteMapData = {
    if let path = Bundle.main.path(forResource: "light_rail_system_map", ofType: "json") {
        do {
            return RouteMapData.Companion().fromString(input: try String(contentsOfFile: path, encoding: .utf8))
        } catch {
        }
    }
    return RouteMapData.init(width: 500, height: 1250, stations: [:])
}()

extension RouteMapData {
    
    func cgWidth() -> CGFloat {
        return CGFloat(width)
    }
    
    func cgHeight() -> CGFloat {
        return CGFloat(height)
    }
    
}

private var mtrOffset: CGPoint? = nil
private var mtrScale: CGFloat? = 0.2
private var lrtOffset: CGPoint? = nil
private var lrtScale: CGFloat? = 0.09

struct TrainRouteMapView: AppScreenView {
    
    @Environment(\.isLuminanceReduced) var ambientMode
    @StateObject private var typhoonInfoState: StateFlowObservable<Registry.TyphoonInfo>
    @ObservedObject private var locationManager = SingleLocationManager()
    
    private let appContext: AppActiveContextWatchOS
    private let storage: KotlinMutableDictionary<NSString, AnyObject>
    
    @State var type: String
    @State var highlightPosition: CGPoint?
    @State var locationJumped: Bool
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.storage = storage
        self.type = data["type"] as? String ?? "MTR"
        self._typhoonInfoState = StateObject(wrappedValue: StateFlowObservable(stateFlow: registry(appContext).typhoonInfo, initSubscribe: true))
        self._locationJumped = State(initialValue: storage["locationJumped"] as? Bool ?? false)
    }
    
    var body: some View {
        Group {
            if type == "MTR" {
                let imageName = {
                    if typhoonInfoState.state.isAboveTyphoonSignalNine {
                        return ambientMode ? "mtr_system_map_watch_typhoon_ambient" : "mtr_system_map_watch_typhoon"
                    } else {
                        return ambientMode ? "mtr_system_map_watch_ambient" : "mtr_system_map_watch"
                    }
                }()
                TrainRouteMapImage(imageSize: CGSize(width: mtrRouteMapData.cgWidth(), height: mtrRouteMapData.cgHeight()), image: Image(imageName), highlightPosition: highlightPosition, locationJumped: locationJumped, minScale: 0.05, maxScale: 0.25, initalScale: mtrScale, initalOffset: mtrOffset, onTap: { position in
                    let stopId = mtrRouteMapData.findClickedStations(x: Float(position.x), y: Float(position.y))
                    if stopId != nil {
                        let stop = RouteExtensionsKt.asStop(stopId!, context: appContext)
                        let data = newAppDataConatiner()
                        let result = registry(appContext).findRoutesBlocking(input: "", exact: false) { r in
                            (Shared().MTR_ROUTE_FILTER(r).boolValue && r.stops[Operator.Companion().MTR]?.contains(stopId!) == true).asKt()
                        }
                        result.forEach {
                            $0.stopInfo = StopInfo(stopId: stopId!, data: stop, distance: 0, co: Operator.Companion().MTR)
                        }
                        data["result"] = StopIndexedRouteSearchResultEntryKt.toStopIndexed(result, instance: appContext)
                        data["listType"] = RouteListType.Companion().NORMAL
                        data["showEta"] = true
                        data["mtrSearch"] = stopId
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listRoutes, data))
                    }
                }, onPan: { position in
                    mtrOffset = position
                }, onScale: { scale in
                    mtrScale = scale
                }, onLocationJumped: {
                    locationJumped = true
                    storage["locationJumped"] = true
                })
                    .ignoresSafeArea(.all)
                    .onChange(of: locationManager.isLocationFetched) { _ in
                        if locationManager.location != nil {
                            let location = locationManager.location!.toLocationResult().location!
                            var allStops: [String: Stop?] = [:]
                            mtrRouteMapData.stations.keys.forEach {
                                allStops[$0] = RouteExtensionsKt.asStop($0, context: appContext)
                            }
                            let optStop = allStops.filter { $0.value != nil }.min { $0.value!.location.distance(other: location) < $1.value!.location.distance(other: location) }
                            if let stop = optStop {
                                let position = mtrRouteMapData.stations[stop.key]!
                                highlightPosition = CGPoint(x: CGFloat(truncating: position.first!), y: CGFloat(truncating: position.second!))
                            }
                        }
                    }
            } else {
                TrainRouteMapImage(imageSize: CGSize(width: lightRailRouteMapData.cgWidth(), height: lightRailRouteMapData.cgHeight()), image: Image(ambientMode ? "light_rail_system_map_watch_ambient" : "light_rail_system_map_watch"), highlightPosition: highlightPosition, locationJumped: locationJumped, minScale: 0.04, maxScale: 0.11, initalScale: lrtScale, initalOffset: lrtOffset, onTap: { position in
                    let stopId = lightRailRouteMapData.findClickedStations(x: Float(position.x), y: Float(position.y))
                    if stopId != nil {
                        let stop = RouteExtensionsKt.asStop(stopId!, context: appContext)
                        let data = newAppDataConatiner()
                        let result = registry(appContext).findRoutesBlocking(input: "", exact: false) { r in
                            (RouteExtensionsKt.firstCo(r.co) == Operator.Companion().LRT && r.stops[Operator.Companion().LRT]?.contains(stopId!) == true).asKt()
                        }
                        result.forEach {
                            $0.stopInfo = StopInfo(stopId: stopId!, data: stop, distance: 0, co: Operator.Companion().LRT)
                        }
                        data["result"] = StopIndexedRouteSearchResultEntryKt.toStopIndexed(result, instance: appContext)
                        data["listType"] = RouteListType.Companion().NORMAL
                        data["showEta"] = true
                        data["mtrSearch"] = stopId
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listRoutes, data))
                    }
                }, onPan: { position in
                    lrtOffset = position
                }, onScale: { scale in
                    lrtScale = scale
                }, onLocationJumped: {
                    locationJumped = true
                    storage["locationJumped"] = true
                })
                    .ignoresSafeArea(.all)
                    .onChange(of: locationManager.isLocationFetched) { _ in
                        if locationManager.location != nil {
                            let location = locationManager.location!.toLocationResult().location!
                            var allStops: [String: Stop?] = [:]
                            lightRailRouteMapData.stations.keys.forEach {
                                allStops[$0] = RouteExtensionsKt.asStop($0, context: appContext)
                            }
                            let optStop = allStops.filter { $0.value != nil }.min { $0.value!.location.distance(other: location) < $1.value!.location.distance(other: location) }
                            if let stop = optStop {
                                let position = lightRailRouteMapData.stations[stop.key]!
                                highlightPosition = CGPoint(x: CGFloat(truncating: position.first!), y: CGFloat(truncating: position.second!))
                            }
                        }
                    }
            }
        }
        .overlay {
            if #unavailable(watchOS 10.0) {
                Text(Shared().language == "en" ? "Interactive system maps are not support prior to watchOS 10" : "WatchOS 10 以下不支援互動式路綫圖")
                    .multilineTextAlignment(.center)
                    .foregroundColor(Color.red)
                    .lineLimit(2)
                    .autoResizing(maxSize: 12.scaled(appContext, true))
                    .padding(.horizontal, 5)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .allowsHitTesting(false)
            }
        }
        .onChange(of: locationManager.readyForRequest) { _ in
            locationManager.requestLocation()
        }
        .onAppear {
            if locationManager.readyForRequest {
                locationManager.requestLocation()
            } else if !locationManager.authorizationDenied {
                locationManager.requestPermission()
            }
        }
    }
}
