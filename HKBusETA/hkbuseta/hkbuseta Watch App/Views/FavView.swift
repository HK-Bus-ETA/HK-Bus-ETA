//
//  EtaView.swift
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

struct FavView: View {
    
    @StateObject private var maxFavItems = FlowStateObservable(defaultValue: KotlinInt(int: Shared().suggestedMaxFavouriteRouteStopState), nativeFlow: Shared().suggestedMaxFavouriteRouteStopStateFlow)
    @State private var showRouteListViewButton = false
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [Int] = []
    @State private var etaResults: ETAResultsContainer<KotlinInt> = ETAResultsContainer()
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var origin: LocationResult? = nil
    
    @State private var favRouteStops: [Int: FavouriteRouteStop] = [:]
    @State private var deleteStates: [Int] = []
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {

    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                VStack(alignment: .center, spacing: 1.scaled()) {
                    Spacer(minLength: 10.scaled())
                    VStack(alignment: .center) {
                        Text(Shared().language == "en" ? "Favourite Routes" : "最喜愛路線")
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled(), weight: .bold)
                    }
                    Spacer(minLength: 10.scaled())
                    if showRouteListViewButton {
                        Button(action: {
                            let data = newAppDataConatiner()
                            data["usingGps"] = !locationManager.authorizationDenied
                            appContext().appendStack(screen: AppScreen.favRouteListView, mutableData: data)
                        }) {
                            Text(Shared().language == "en" ? "Route List View" : "路線一覽列表")
                                .font(.system(size: 17.scaled(), weight: .bold))
                        }
                        .frame(width: 160.scaled(), height: 35.scaled())
                        .clipShape(RoundedRectangle(cornerRadius: 25))
                        .edgesIgnoringSafeArea(.all)
                        Spacer(minLength: 10.scaled())
                    }
                    ForEach(1..<(Int(truncating: maxFavItems.state) + 1), id: \.self) { index in
                        FavButton(favIndex: index).id(index)
                        Spacer(minLength: 5.scaled())
                    }
                }
            }
        }
        .onChange(of: maxFavItems.state) { _ in
            showRouteListViewButton = Shared().shouldShowFavListRouteView
        }
        .onAppear {
            showRouteListViewButton = Shared().shouldShowFavListRouteView
        }
        .onReceive(etaTimer) { _ in
            for favIndex in etaActive {
                let currentFavRouteStop = favRouteStops[favIndex] ?? {
                    let s = Shared().getFavouriteRouteStop(index: favIndex.asInt32())
                    DispatchQueue.main.async {
                        favRouteStops[favIndex] = s
                    }
                    return s
                }()
                if currentFavRouteStop != nil {
                    fetchEta(stopId: currentFavRouteStop!.stopId, stopIndex: currentFavRouteStop!.index, co: currentFavRouteStop!.co, route: currentFavRouteStop!.route) { etaResults.set(key: favIndex.asKt(), result: $0) }
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
                origin = locationManager.location!.coordinate.toLocationResult()
            }
        }
    }
    
    func FavButton(favIndex: Int) -> some View {
        let currentFavRouteStop = favRouteStops[favIndex] ?? {
            let s = Shared().getFavouriteRouteStop(index: favIndex.asInt32())
            DispatchQueue.main.async {
                favRouteStops[favIndex] = s
            }
            return s
        }()
        let deleteState = deleteStates.contains { $0 == favIndex }
        return Button(action: {}) {
            HStack(alignment: .top, spacing: 0) {
                ZStack(alignment: .leading) {
                    Text("")
                        .frame(width: 35)
                    ZStack {
                        Circle()
                            .fill(currentFavRouteStop != nil ? colorInt(0xFF3D3D3D).asColor() : colorInt(0xFF131313).asColor())
                            .frame(width: 30.scaled(), height: 30.scaled())
                        if deleteState {
                            Image(systemName: "xmark")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 17.scaled(), height: 17.scaled())
                                .foregroundColor(colorInt(0xFFFF0000).asColor())
                        } else {
                            Text("\(favIndex)")
                                .font(.system(size: 17.scaled(), weight: .bold))
                                .frame(width: 17.scaled(), height: 17.scaled())
                                .foregroundColor(currentFavRouteStop != nil ? colorInt(0xFFFFFF00).asColor() : colorInt(0xFF444444).asColor())
                        }
                    }
                }
                .overlay(alignment: .leading) {
                    if currentFavRouteStop != nil {
                        ETAElement(favIndex: favIndex, currentFavRouteStop: currentFavRouteStop!)
                            .offset(y: 30.scaled())
                    }
                }
                .padding(10)
                VStack(alignment: .leading, spacing: 1.scaled()) {
                    if currentFavRouteStop == nil {
                        HStack(alignment: .center) {
                            Text(Shared().language == "en" ? "No Route Selected" : "未有設置路線")
                                .font(.system(size: 16.scaled(), weight: .bold))
                                .foregroundColor(colorInt(0xFF505050).asColor())
                                .lineLimit(2)
                                .lineSpacing(0)
                                .multilineTextAlignment(.leading)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .fixedSize(horizontal: false, vertical: true)
                        }.frame(maxHeight: .infinity)
                    } else {
                        let resolvedStop = currentFavRouteStop!.resolveStop(context: appContext()) { origin?.location }
                        let stopName = resolvedStop.stop.name
                        let index = resolvedStop.index
                        let route = resolvedStop.route
                        let co = currentFavRouteStop!.co
                        let routeNumber = route.routeNumber
                        let gpsStop = currentFavRouteStop!.favouriteStopMode.isRequiresLocation
                        let destName = registry().getStopSpecialDestinations(stopId: currentFavRouteStop!.stopId, co: currentFavRouteStop!.co, route: route, prependTo: true)
                        let color = currentFavRouteStop!.co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64)
                        let operatorName = currentFavRouteStop!.co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: route.isKmbCtbJoint, language: Shared().language, elseName: "???")
                        let mainText = "\(operatorName) \(currentFavRouteStop!.co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))"
                        let routeText = destName.get(language: Shared().language)
                        
                        let subText = {
                            var text = ((co.isTrain ? "" : "\(index). ") + stopName.get(language: Shared().language)).asAttributedString()
                            if gpsStop {
                                text += (Shared().language == "en" ? " - Closest" : " - 最近").asAttributedString(color: colorInt(0xFFFFE496).asColor(), fontSize: 12 * 0.8)
                            }
                            return text
                        }()
                        
                        VStack(alignment: .leading, spacing: 0) {
                            MarqueeText(
                                text: mainText,
                                font: UIFont.systemFont(ofSize: 19.scaled(), weight: .bold),
                                leftFade: 8.scaled(),
                                rightFade: 8.scaled(),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(color.asColor())
                            .lineLimit(1)
                            MarqueeText(
                                text: routeText,
                                font: UIFont.systemFont(ofSize: 17.scaled()),
                                leftFade: 8.scaled(),
                                rightFade: 8.scaled(),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(.white)
                            .lineLimit(1)
                            Spacer(minLength: 3.scaled())
                            MarqueeText(
                                text: subText,
                                font: UIFont.systemFont(ofSize: 14.scaled()),
                                leftFade: 8.scaled(),
                                rightFade: 8.scaled(),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(.white)
                            .lineLimit(1)
                        }
                    }
                }.padding(.vertical, 5)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .background { deleteState ? colorInt(0xFF633A3A).asColor() : colorInt(0xFF1A1A1A).asColor() }
        .frame(minWidth: 178.0.scaled(), maxWidth: 178.0.scaled(), minHeight: 47.0.scaled())
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    playHaptics()
                    if deleteState {
                        deleteStates.removeAll { $0 == favIndex }
                    } else {
                        deleteStates.append(favIndex)
                    }
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if deleteState {
                        if (registry().hasFavouriteRouteStop(favoriteIndex: favIndex.asInt32())) {
                            registry().clearFavouriteRouteStop(favoriteIndex: favIndex.asInt32(), context: appContext())
                        }
                        DispatchQueue.main.async {
                            deleteStates.removeAll { $0 == favIndex }
                            favRouteStops[favIndex] = Shared().getFavouriteRouteStop(index: favIndex.asInt32())
                        }
                    } else {
                        if currentFavRouteStop != nil {
                            let co = currentFavRouteStop!.co
                            let resolvedStop = currentFavRouteStop!.resolveStop(context: appContext()) { origin?.location }
                            let index = resolvedStop.index
                            let stopId = resolvedStop.stopId
                            let stop = resolvedStop.stop
                            let route = resolvedStop.route
                            let entry = registry().findRoutes(input: route.routeNumber, exact: true, predicate: {
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
                            if !entry.isEmpty {
                                let data = newAppDataConatiner()
                                data["route"] = entry[0]
                                data["scrollToStop"] = stopId
                                appContext().appendStack(screen: AppScreen.listStops, mutableData: data)
                            }
                            
                            let data = newAppDataConatiner()
                            data["stopId"] = stopId
                            data["co"] = co
                            data["index"] = index
                            data["stop"] = stop
                            data["route"] = route
                            appContext().appendStack(screen: AppScreen.eta, mutableData: data)
                        }
                    }
                }
        )
    }
    
    func ETAElement(favIndex: Int, currentFavRouteStop: FavouriteRouteStop) -> some View {
        Group {
            FavEtaView(etaState: etaResults.getState(key: favIndex.asKt()))
            Text("")
        }
        .onAppear {
            etaActive.append(favIndex)
            fetchEta(stopId: currentFavRouteStop.stopId, stopIndex: currentFavRouteStop.index, co: currentFavRouteStop.co, route: currentFavRouteStop.route) { etaResults.set(key: favIndex.asKt(), result: $0) }
        }
        .onDisappear {
            etaActive.removeAll(where: { $0 == favIndex })
        }
    }

}

struct FavEtaView: View {
    
    @StateObject private var etaState: FlowStateObservable<Registry.ETAQueryResult?>
    
    init(etaState: ETAResultsState) {
        self._etaState = StateObject(wrappedValue: FlowStateObservable(defaultValue: etaState.state, nativeFlow: etaState.stateFlow))
    }
    
    var body: some View {
        let optEta = etaState.state
        if optEta != nil {
            let eta = optEta!
            if !eta.isConnectionError {
                if !(0..<60).contains(eta.nextScheduledBus) {
                    if eta.isMtrEndOfLine {
                        Image(systemName: "arrow.forward.to.line.circle")
                            .font(.system(size: 17.scaled()))
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                    } else if (eta.isTyphoonSchedule) {
                        Image(systemName: "hurricane")
                            .font(.system(size: 17.scaled()))
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                    } else {
                        Image(systemName: "clock")
                            .font(.system(size: 17.scaled()))
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                    }
                } else {
                    let shortText = eta.firstLine.shortText
                    let text1 = shortText.first
                    let text2 = shortText.second
                    let text = text1.asAttributedString(fontSize: 17.scaled()) + text2.asAttributedString(fontSize: 8.scaled())
                    Text(text)
                        .multilineTextAlignment(.leading)
                        .lineSpacing(0)
                        .frame(alignment: .leading)
                        .foregroundColor(colorInt(0xFF92C6F0).asColor())
                        .lineLimit(1)
                }
            }
        }
    }
    
}

#Preview {
    FavView(data: [:], storage: KotlinMutableDictionary())
}
