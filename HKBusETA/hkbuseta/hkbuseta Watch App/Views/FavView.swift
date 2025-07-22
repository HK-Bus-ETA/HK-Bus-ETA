//
//  EtaView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared

struct FavView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = StateFlowObservable(stateFlow: Shared().jointOperatedColorFractionState)
    
    @StateObject private var favouriteRouteStops = StateFlowListObservable(stateFlow: Shared().favoriteRouteStops)
    @State private var showRouteListViewButton = false
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [Int] = []
    @State private var etaResults: ETAResultsContainer<KotlinInt> = ETAResultsContainer()
    
    let deleteTimer = Timer.publish(every: 0.2, on: .main, in: .common).autoconnect()
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var origin: LocationResult? = nil
    
    @State private var favRouteStops: [Int: FavouriteRouteStop] = [:]
    @State private var deleteStates: [Int: Double] = [:]
    @State private var selectedGroup: BilingualText
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.selectedGroup = typedValue(Shared().favoriteRouteStops).first!.name
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                LazyVStack(alignment: .center, spacing: 1.scaled(appContext)) {
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    Text(Shared().language == "en" ? "Favourite Routes" : "收藏路線")
                        .multilineTextAlignment(.center)
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                        .lineLimit(2)
                        .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
                    Spacer().frame(height: 5.scaled(appContext))
                    Text(Shared().language == "en" ? "Routes can be displayed in Tiles" : "路線可在資訊方塊中顯示")
                        .font(.system(size: 10.scaled(appContext, true)))
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled(appContext))
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    if showRouteListViewButton {
                        Button(action: {
                            let data = newAppDataConatiner()
                            data["usingGps"] = !locationManager.authorizationDenied
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.favRouteListView, data))
                        }) {
                            Text(Shared().language == "en" ? "Route List View" : "路線一覽列表")
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                                .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                        }
                        .frame(width: 160.scaled(appContext), height: 35.scaled(appContext))
                        .clipShape(RoundedRectangle(cornerRadius: 25))
                        .buttonStyle(PlainButtonStyle())
                        .background {
                            colorInt(0xFF1A1A1A)
                                .asColor()
                                .clipShape(RoundedRectangle(cornerRadius: 25))
                        }
                        .ignoresSafeArea(.all)
                        Spacer().frame(fixedSize: 5.scaled(appContext))
                    }
                    Button(action: {
                        let current = selectedGroup
                        let index = favouriteRouteStops.state.firstIndex { $0.name == current }!
                        selectedGroup = index + 1 >= favouriteRouteStops.state.count ? favouriteRouteStops.state.first!.name : favouriteRouteStops.state[index + 1].name
                    }) {
                        UserMarqueeText(
                            text: selectedGroup.get(language: Shared().language),
                            font: UIFont.systemFont(ofSize: 17.scaled(appContext, true), weight: .bold),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .center
                        )
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        .lineLimit(1)
                    }
                    .frame(width: 160.scaled(appContext), height: 35.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    let routeStops = FavouriteRouteGroupKt.getByName(favouriteRouteStops.state, name: selectedGroup)!.favouriteRouteStops
                    if routeStops.isEmpty {
                        Text(Shared().language == "en" ? "No favourite routes" : "沒有收藏路線")
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled(appContext, true))
                    } else {
                        ForEach(routeStops.withIndex(), id: \.1.favouriteId) { (index, routeStop) in
                            FavButton(numIndex: index + 1, favouriteRouteStop: routeStop)
                                .id(routeStop.favouriteId)
                            Spacer().frame(fixedSize: 5.scaled(appContext))
                        }
                        .animation(.default, value: favouriteRouteStops.state)
                    }
                }
            }
        }
        .onAppear {
            favouriteRouteStops.subscribe()
        }
        .onDisappear {
            jointOperatedColorFraction.unsubscribe()
            favouriteRouteStops.unsubscribe()
        }
        .onChange(of: favouriteRouteStops.state) { _ in
            showRouteListViewButton = Shared().shouldShowFavListRouteView
        }
        .onAppear {
            showRouteListViewButton = Shared().shouldShowFavListRouteView
        }
        .onReceive(etaTimer) { _ in
            for favIndex in etaActive {
                let favouriteRouteStop = favRouteStops[favIndex] ?? {
                    let s = FavouriteRouteGroupKt.getFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: favIndex.asInt32())
                    DispatchQueue.main.async {
                        favRouteStops[favIndex] = s
                    }
                    return s
                }()
                if favouriteRouteStop != nil {
                    fetchEta(appContext: appContext, stopId: favouriteRouteStop!.stopId, stopIndex: favouriteRouteStop!.index, co: favouriteRouteStop!.co, route: favouriteRouteStop!.route) { etaResults.set(key: favIndex.asKt(), result: $0) }
                }
            }
        }
        .onReceive(deleteTimer) { _ in
            for (favIndex, time) in deleteStates {
                let newTime = time - 0.2
                DispatchQueue.main.async {
                    if newTime > 0 {
                        deleteStates[favIndex] = newTime
                    } else {
                        deleteStates.removeValue(forKey: favIndex)
                    }
                }
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
        .onChange(of: locationManager.isLocationFetched) { _ in
            if locationManager.location != nil {
                origin = locationManager.location!.toLocationResult()
            }
        }
    }
    
    func FavButton(numIndex: Int, favouriteRouteStop: FavouriteRouteStop) -> some View {
        let favIndex = favouriteRouteStop.favouriteId
        DispatchQueue.main.async {
            favRouteStops[Int(favIndex)] = favouriteRouteStop
        }
        let anyTileUses = Tiles().getTileUseState(index: favIndex)
        let deleteState = deleteStates[Int(favIndex)] ?? 0.0
        return Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: 0) {
                    ZStack(alignment: .leading) {
                        Text("").frame(width: 32.scaled(appContext, true))
                        ZStack {
                            Circle()
                                .fill(colorInt(0xFF3D3D3D).asColor())
                                .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                                .overlay {
                                    if deleteState > 0.0 {
                                        Circle()
                                            .trim(from: 0.0, to: deleteState / 5.0)
                                            .rotation(.degrees(-90))
                                            .stroke(colorInt(0xFFFF0000).asColor(), style: StrokeStyle(lineWidth: 2.scaled(appContext), lineCap: .butt))
                                            .animation(.linear, value: deleteState)
                                    }
                                }
                            if deleteState > 0.0 {
                                Image(systemName: "xmark")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 13.scaled(appContext, true), height: 13.scaled(appContext, true))
                                    .foregroundColor(colorInt(0xFFFF0000).asColor())
                            } else {
                                Text("\(numIndex)")
                                    .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                                    .foregroundColor(colorInt(0xFFFFFF00).asColor())
                            }
                        }
                    }
                    .padding([.top, .leading, .trailing], 10)
                    Spacer()
                    ETAElement(favIndex: Int(favIndex), favouriteRouteStop: favouriteRouteStop)
                        .padding(.bottom, 5.scaled(appContext))
                        .padding(.leading, 10)
                        .frame(alignment: .bottomLeading)
                }
                VStack(alignment: .leading, spacing: 1.scaled(appContext)) {
                    let resolvedStop = favouriteRouteStop.resolveStop(context: appContext) { origin?.location }
                    let stopName = resolvedStop.stop.name
                    let stopId = resolvedStop.stopId
                    let index = resolvedStop.index.asInt()
                    let route = resolvedStop.route
                    let kmbCtbJoint = route.isKmbCtbJoint
                    let co = favouriteRouteStop.co
                    let routeNumber = route.routeNumber
                    let gmbRegion = route.gmbRegion
                    let gpsStop = favouriteRouteStop.favouriteStopMode.isRequiresLocation
                    
                    let stopList = registry(appContext).getAllStops(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion)
                    let stopData = stopList.enumerated().filter { $0.element.stopId == stopId }.min(by: { abs($0.offset - index) < abs($1.offset - index) })?.element
                    let branches = registry(appContext).getAllBranchRoutes(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion, includeFakeRoutes: false)
                    let currentBranch = AppContextWatchOSKt.findMostActiveRoute(TimetableUtilsKt.currentBranchStatus(branches, time: TimeUtilsKt.currentLocalDateTime(), context: appContext, resolveSpecialRemark: false))
                    let destName = {
                        if co.isTrain {
                            return registry(appContext).getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: false)
                        } else if stopData?.branchIds.contains(currentBranch) != false {
                            return route.resolvedDestWithBranch(prependTo: false, branch: currentBranch, selectedStop: index.asInt32(), selectedStopId: stopId, context: appContext)
                        } else {
                            return route.resolvedDest(prependTo: false)
                        }
                    }()
                    
                    let color = operatorColor(favouriteRouteStop.co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }
                    let operatorName = favouriteRouteStop.co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: kmbCtbJoint, gmbRegion: gmbRegion, language: Shared().language, elseName: "???")
                    let mainText = "\(operatorName) \(favouriteRouteStop.co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))"
                    let routeText = destName.get(language: Shared().language)
                    
                    let subText = {
                        var text = ((co.isTrain ? "" : "\(index). ") + stopName.get(language: Shared().language)).asAttributedString()
                        if gpsStop {
                            text += (Shared().language == "en" ? " - Closest" : " - 最近").asAttributedString(color: colorInt(0xFFFFE496).asColor(), fontSize: 14 * 0.8)
                        }
                        return text
                    }()
                    
                    VStack(alignment: .leading, spacing: 0) {
                        UserMarqueeText(
                            text: mainText,
                            font: UIFont.systemFont(ofSize: 19.scaled(appContext, true), weight: Shared().disableBoldDest ? .regular : .bold),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .bottomLeading
                        )
                        .foregroundColor(color.asColor())
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .onAppear {
                            if kmbCtbJoint {
                                jointOperatedColorFraction.subscribe()
                            }
                        }
                        HStack(alignment: .firstTextBaseline, spacing: 0) {
                            if route.shouldPrependTo() {
                                Text(RouteExtensionsKt.bilingualToPrefix.get(language: Shared().language))
                                    .font(.system(size: 13.scaled(appContext, true)))
                                    .foregroundColor(.white)
                            }
                            UserMarqueeText(
                                text: routeText,
                                font: UIFont.systemFont(ofSize: 17.scaled(appContext, true), weight: Shared().disableBoldDest ? .regular : .bold),
                                marqueeStartDelay: 2,
                                marqueeAlignment: .leadingFirstTextBaseline
                            )
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        Spacer().frame(fixedSize: 3.scaled(appContext))
                        UserMarqueeText(
                            text: subText,
                            font: UIFont.systemFont(ofSize: 14.scaled(appContext, true)),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .leadingFirstTextBaseline
                        )
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.vertical, 5)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .background { deleteState > 0.0 ? colorInt(0xFF633A3A).asColor() : colorInt(0xFF1A1A1A).asColor() }
        .animation(.linear(duration: 0.25), value: deleteState)
        .frame(minWidth: 178.0.scaled(appContext), maxWidth: 178.0.scaled(appContext), minHeight: 47.0.scaled(appContext))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .tileStateBorder(anyTileUses, 10)
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    if deleteState <= 0.0 {
                        playHaptics()
                        appContext.showToastText(text: Shared().language == "en" ? "Click again to confirm delete" : "再次點擊確認刪除", duration: ToastDuration.short_)
                        deleteStates[Int(favIndex)] = 5.0
                    }
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if deleteState > 0.0 {
                        if (FavouriteRouteGroupKt.getFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: favIndex) != nil) {
                            registry(appContext).setFavouriteRouteGroups(favouriteRouteStops: FavouriteRouteGroupKt.removeFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: favIndex), context: appContext)
                            appContext.showToastText(text: Shared().language == "en" ? "Cleared Favourite Route \(numIndex)" : "已清除收藏路線\(numIndex)", duration: ToastDuration.short_)
                        }
                        DispatchQueue.main.async {
                            deleteStates.removeValue(forKey: Int(favIndex))
                            favRouteStops[Int(favIndex)] = FavouriteRouteGroupKt.getFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: favIndex)
                        }
                    } else {
                        let co = favouriteRouteStop.co
                        let resolvedStop = favouriteRouteStop.resolveStop(context: appContext) { origin?.location }
                        let index = resolvedStop.index
                        let stopId = resolvedStop.stopId
                        let stop = resolvedStop.stop
                        let route = resolvedStop.route
                        let entry = registry(appContext).findRoutesBlocking(input: route.routeNumber, exact: true, predicate: {
                            let bound = $0.bound
                            if !bound.keys.contains(where: { $0 == co }) || bound[co] != route.bound[co] {
                                return false.asKt()
                            }
                            let stops = $0.stops[co]
                            if stops == nil {
                                return false.asKt()
                            }
                            return stops!.contains { $0 == stopId }.asKt()
                        })
                        
                        let routeKey = route.getRouteKey(context: appContext)
                        if routeKey != nil {
                            registry(appContext).addLastLookupRoute(routeKey: routeKey!, context: appContext)
                        }
                        
                        if !entry.isEmpty {
                            let data = newAppDataConatiner()
                            data["route"] = entry[0]
                            data["scrollToStop"] = stopId
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.listStops, data))
                        }
                        
                        let data = newAppDataConatiner()
                        data["stopId"] = stopId
                        data["co"] = co
                        data["index"] = index
                        data["stop"] = stop
                        data["route"] = route
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                    }
                }
        )
    }
    
    func ETAElement(favIndex: Int, favouriteRouteStop: FavouriteRouteStop) -> some View {
        ZStack {
            FavEtaView(appContext: appContext, etaState: etaResults.getState(key: favIndex.asKt()))
        }
        .onAppear {
            etaActive.append(favIndex)
            fetchEta(appContext: appContext, stopId: favouriteRouteStop.stopId, stopIndex: favouriteRouteStop.index, co: favouriteRouteStop.co, route: favouriteRouteStop.route) { etaResults.set(key: favIndex.asKt(), result: $0) }
        }
        .onDisappear {
            etaActive.removeAll(where: { $0 == favIndex })
        }
    }

}

struct FavEtaView: View {
    
    @StateObject private var etaState: StateFlowNullableObservable<Registry.ETAQueryResult>
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, etaState: ETAResultsState) {
        self.appContext = appContext
        self._etaState = StateObject(wrappedValue: StateFlowNullableObservable(stateFlow: etaState.state))
    }
    
    var body: some View {
        ZStack {
            let optEta = etaState.state
            if optEta != nil {
                let eta = optEta!
                if !eta.isConnectionError {
                    if !(0..<60).contains(eta.nextScheduledBus) {
                        if eta.isMtrEndOfLine {
                            Image(systemName: "arrow.forward.to.line.circle")
                                .font(.system(size: 17.scaled(appContext, true)))
                                .foregroundColor(colorInt(0xFF92C6F0).asColor())
                        } else if (eta.isTyphoonSchedule) {
                            Image(systemName: "hurricane")
                                .font(.system(size: 17.scaled(appContext, true)))
                                .foregroundColor(colorInt(0xFF92C6F0).asColor())
                        } else {
                            Image(systemName: "clock")
                                .font(.system(size: 17.scaled(appContext, true)))
                                .foregroundColor(colorInt(0xFF92C6F0).asColor())
                        }
                    } else {
                        let text: AttributedString = {
                            if (Shared().etaDisplayMode.wearableShortTextClockTime) {
                                let text1 = Shared().getResolvedText(eta, seq: 1.asInt32(), etaDisplayMode: Shared().etaDisplayMode, context: appContext).resolvedClockTime.string.trimmingCharacters(in: .whitespaces)
                                return text1.replace("\\s+", "\n").asAttributedString(fontSize: 13.scaled(appContext, true))
                            } else {
                                let shortText = eta.firstLine.shortText
                                let text1 = shortText.first
                                let text2 = shortText.second
                                return text1.asAttributedString(fontSize: 17.scaled(appContext, true)) + text2.asAttributedString(fontSize: 8.scaled(appContext, true))
                            }
                        }()
                        Text(text)
                            .multilineTextAlignment(.leading)
                            .lineSpacing(0)
                            .frame(alignment: .leading)
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                    }
                }
            }
        }
        .onAppear {
            etaState.subscribe()
        }
        .onDisappear {
            etaState.unsubscribe()
        }
    }
    
}
