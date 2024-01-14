//
//  EtaTileListView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 13/01/2024.
//

import SwiftUI
import shared
import Combine

struct EtaTileListView: AppScreenView {
    
    private let appContext: AppActiveContextWatchOS
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var origin: LocationResult? = nil
    
    @State private var etaTileIds: [Int32]
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.etaTileIds = Tiles().getEtaTileConfigurationsIds().map { Int32(truncating: $0) }.sorted()
    }
    
    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .center) {
                Spacer(minLength: 20.scaled(appContext))
                ForEach(etaTileIds.indices, id: \.self) { index in
                    EtaTileView(appContext: appContext, origin: _origin, tileId: etaTileIds[index], etaTimer: etaTimer)
                    Spacer(minLength: 10.scaled(appContext))
                }
                if etaTileIds.count < 15 {
                    Button(action: {}) {
                        Text("+")
                            .font(.system(size: 23.scaled(appContext), weight: .bold))
                            .foregroundColor(.gray)
                            .frame(width: 178.0.scaled(appContext), height: 67.0.scaled(appContext))
                    }
                    .buttonStyle(PlainButtonStyle())
                    .background { colorInt(0xFF1A1A1A).asColor() }
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .highPriorityGesture(
                        TapGesture()
                            .onEnded { _ in
                                let tileId = (etaTileIds.max() ?? -1) + 1
                                let data = newAppDataConatiner()
                                data["tileId"] = tileId
                                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileConfigure, data))
                            }
                    )
                }
            }
        }
        .onAppear {
            self.etaTileIds = Tiles().getEtaTileConfigurationsIds().map { Int32(truncating: $0) }.sorted()
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
        .onChange(of: locationManager.isLocationFetched) { _ in
            if locationManager.location != nil {
                origin = locationManager.location!.coordinate.toLocationResult()
            }
        }
        .onReceive(etaTimer) { _ in
            if locationManager.readyForRequest {
                locationManager.isLocationFetched = false
                locationManager.requestLocation()
            }
        }
    }
}

struct EtaTileView: View {
    
    let etaTimer: Publishers.Autoconnect<Timer.TimerPublisher>
    
    private let appContext: AppActiveContextWatchOS
    @State private var origin: LocationResult?
    private let tileId: Int32
    
    private let favouriteRouteStops: [FavouriteRouteStop]
    
    @State private var mergedEtaCombiner: MergedETACombiner
    @StateObject private var mergedState: FlowStateObservable<MergedETAContainer>
    @State private var lastUpdated: Kotlinx_datetimeLocalDateTime = TimeUtilsKt.currentLocalDateTime()
    
    init(appContext: AppActiveContextWatchOS, origin: State<LocationResult?>, tileId: Int32, etaTimer: Publishers.Autoconnect<Timer.TimerPublisher>) {
        self.appContext = appContext
        self._origin = origin
        self.tileId = tileId
        self.etaTimer = etaTimer
        self.favouriteRouteStops = Tiles().getEtaTileConfiguration(tileId: tileId).map { Shared().getFavouriteRouteStop(index: Int32(truncating: $0)) }.filter { $0 != nil }.map { $0! }
        let mergedEtaCombiner = MergedETACombiner(size: favouriteRouteStops.count.asInt32())
        self.mergedEtaCombiner = mergedEtaCombiner
        self._mergedState = StateObject(wrappedValue: FlowStateObservable(defaultValue: mergedEtaCombiner.mergedState, nativeFlow: mergedEtaCombiner.mergedStateFlow))
    }
    
    var body: some View {
        let eta = mergedState.state.eta
        let mainResolvedStop = eta?.firstKey ?? {
            let favStop = favouriteRouteStops[0]
            return KotlinPair(first: favStop.resolveStop(context: appContext) { origin?.location }, second: favStop)
        }()
        let favouriteResolvedStop = mainResolvedStop.first!
        let favouriteStopRoute = mainResolvedStop.second!
        
        let index = favouriteResolvedStop.index
        let stopId = favouriteResolvedStop.stopId
        let stop = favouriteResolvedStop.stop
        let route = favouriteResolvedStop.route
        
        let co = favouriteStopRoute.co
        let gpsStop = favouriteStopRoute.favouriteStopMode.isRequiresLocation

        let routeNumber = route.routeNumber
        let destName = registryNoUpdate(appContext).getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true)
        
        let color: Color = {
            if eta == nil {
                return co.getColor(routeNumber: routeNumber, elseColor: 0xFFCCCCCC).asColor().adjustBrightness(percentage: 0.5)
            } else if eta!.isConnectionError {
                return colorInt(0xFF444444).asColor()
            } else {
                return eta!.nextCo.getColor(routeNumber: routeNumber, elseColor: 0xFFCCCCCC).asColor().adjustBrightness(percentage: !(0...59).contains(eta!.nextScheduledBus) ? 0.2 : 1)
            }
        }()
        
        Button(action: {}) {
            HStack {
                Rectangle()
                    .frame(width: 4)
                    .foregroundColor(color)
                    .animation(.linear(duration: 1.0), value: color)
                VStack(alignment: .leading, spacing: 0) {
                    Spacer(minLength: 2.scaled(appContext))
                    Text(co.isTrain ? stop.name.get(language: Shared().language) : "\(index). \(stop.name.get(language: Shared().language))")
                        .multilineTextAlignment(.leading)
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        .lineLimit(2)
                        .autoResizing(maxSize: 21.scaled(appContext), weight: .bold)
                        .highPriorityGesture(
                            TapGesture()
                                .onEnded { _ in
                                    let data = newAppDataConatiner()
                                    data["stopId"] = stopId
                                    data["co"] = co
                                    data["index"] = index
                                    data["stop"] = stop
                                    data["route"] = route
                                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                                }
                        )
                    Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false) + " " + destName.get(language: Shared().language) + (gpsStop ? (Shared().language == "en" ? " - Closest" : " - 最近") : ""))
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        .lineLimit(1)
                        .autoResizing(maxSize: 12.scaled(appContext))
                        .highPriorityGesture(
                            TapGesture()
                                .onEnded { _ in
                                    let data = newAppDataConatiner()
                                    data["stopId"] = stopId
                                    data["co"] = co
                                    data["index"] = index
                                    data["stop"] = stop
                                    data["route"] = route
                                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                                }
                        )
                    Spacer(minLength: 2.scaled(appContext))
                    ETALine(lines: eta, seq: 1, mainResolvedStop: mainResolvedStop)
                    ETALine(lines: eta, seq: 2, mainResolvedStop: mainResolvedStop)
                    ETALine(lines: eta, seq: 3, mainResolvedStop: mainResolvedStop)
                    Spacer(minLength: 2.scaled(appContext))
                    HStack(spacing: 10.scaled(appContext)) {
                        Text((Shared().language == "en" ? "Updated: " : "更新時間: ") + appContext.formatTime(localDateTime: lastUpdated))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(1)
                            .autoResizing(maxSize: 12.scaled(appContext))
                        Text(Shared().language == "en" ? "Edit" : "編輯")
                            .foregroundColor(colorInt(0xFFFFFF00).asColor())
                            .lineLimit(1)
                            .autoResizing(maxSize: 12.scaled(appContext))
                            .highPriorityGesture(
                                TapGesture()
                                    .onEnded { _ in
                                        let data = newAppDataConatiner()
                                        data["tileId"] = Int(tileId)
                                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileConfigure, data))
                                    }
                            )
                    }
                    Spacer(minLength: 2.scaled(appContext))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .frame(width: 178.0.scaled(appContext), height: 160.0.scaled(appContext))
        }
        .buttonStyle(PlainButtonStyle())
        .background { colorInt(0xFF1A1A1A).asColor() }
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .onReceive(etaTimer) { _ in
            let resolved = RouteExtensionsKt.resolveStops(favouriteRouteStops, context: appContext) { origin?.location }.filter { $0.second != nil }
            mergedEtaCombiner.reset(size: resolved.count.asInt32())
            for r in resolved {
                let favStop = r.first!
                let resolved = r.second!
                let index = resolved.index
                let stopId = resolved.stopId
                let route = resolved.route
                fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: favStop.co, route: route) {
                    mergedEtaCombiner.addResult(result: KotlinPair(first: KotlinPair(first: resolved, second: favStop), second: $0))
                }
            }
        }
        .onChange(of: origin) { origin in
            if origin != nil {
                let resolved = RouteExtensionsKt.resolveStops(favouriteRouteStops, context: appContext) { origin?.location }.filter { $0.second != nil }
                mergedEtaCombiner.reset(size: resolved.count.asInt32())
                for r in resolved {
                    let favStop = r.first!
                    let resolved = r.second!
                    let index = resolved.index
                    let stopId = resolved.stopId
                    let route = resolved.route
                    fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: favStop.co, route: route) {
                        mergedEtaCombiner.addResult(result: KotlinPair(first: KotlinPair(first: resolved, second: favStop), second: $0))
                    }
                }
            }
        }
        .onAppear {
            let resolved = RouteExtensionsKt.resolveStops(favouriteRouteStops, context: appContext) { origin?.location }.filter { $0.second != nil }
            mergedEtaCombiner.reset(size: resolved.count.asInt32())
            for r in resolved {
                let favStop = r.first!
                let resolved = r.second!
                let index = resolved.index
                let stopId = resolved.stopId
                let route = resolved.route
                fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: favStop.co, route: route) {
                    mergedEtaCombiner.addResult(result: KotlinPair(first: KotlinPair(first: resolved, second: favStop), second: $0))
                }
            }
        }
        .onChange(of: mergedState.state) { state in
            if !state.isEmpty() {
                self.lastUpdated = TimeUtilsKt.currentLocalDateTime()
            }
        }
    }
    
    func ETALine(lines: RegistryMergedETAQueryResult<KotlinPair<FavouriteResolvedStop, FavouriteRouteStop>>?, seq: Int, mainResolvedStop: KotlinPair<FavouriteResolvedStop, FavouriteRouteStop>) -> some View {
        let line = Shared().getResolvedText(lines, seq: seq.asInt32(), clockTimeMode: Shared().clockTimeMode, context_: appContext)
        let text = line.second!.asAttributedString(defaultFontSize: 17.scaled(appContext))
        
        let pair = line.first ?? mainResolvedStop
        let resolvedStop = pair.first!
        let favouriteStopRoute = pair.second!
        let index = resolvedStop.index
        let stopId = resolvedStop.stopId
        let stop = resolvedStop.stop
        let route = resolvedStop.route
        let co = favouriteStopRoute.co
        
        return MarqueeText(
            text: text,
            font: UIFont.systemFont(ofSize: 17.scaled(appContext)),
            leftFade: 8.scaled(appContext),
            rightFade: 8.scaled(appContext),
            startDelay: 2,
            alignment: .leading
        )
        .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: lines == nil ? 0.7 : 1))
        .lineLimit(1)
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    let data = newAppDataConatiner()
                    data["stopId"] = stopId
                    data["co"] = co
                    data["index"] = index
                    data["stop"] = stop
                    data["route"] = route
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                }
        )
    }
    
}
