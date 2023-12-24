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
    @State private var etaActive: [String: StopIndexedRouteSearchResultEntry] = [:]
    @State private var etaResults: [String: Registry.ETAQueryResult?] = [:]
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var origin: LocationResult? = nil
    
    init(data: [String: Any]?) {

    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                VStack(alignment: .center, spacing: 1.scaled()) {
                    VStack(alignment: .center) {
                        Text(Shared().language == "en" ? "Favourite Routes" : "最喜愛路線")
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled())
                            .bold()
                        Spacer(minLength: 5.scaled())
                        Text(Shared().language == "en" ? "Routes can be displayed in Tiles" : "路線可在資訊方塊中顯示")
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .lineLimit(1)
                            .autoResizing(maxSize: 12.scaled())
                    }
                    Spacer(minLength: 10.scaled())
                    if showRouteListViewButton {
                        Button(action: {
                            let data = newAppDataConatiner()
                            data["usingGps"] = !locationManager.authorizationDenied
                            appContext().appendStack(screen: AppScreen.favRouteListView, mutableData: data)
                        }) {
                            Text(Shared().language == "en" ? "Route List View" : "路線一覽列表")
                        }
                        .frame(width: 170.scaled(), height: 45.scaled())
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
        .onChange(of: maxFavItems.state) {
            showRouteListViewButton = shouldShowRouteListViewButton()
        }
        .onReceive(etaTimer) { _ in
            for (uniqueKey, entry) in etaActive {
                fetchEta(stopId: entry.stopInfo!.stopId, stopIndex: entry.stopInfoIndex, co: entry.co, route: entry.route!) { etaResults[uniqueKey] = $0 }
            }
        }
    }
    
    func shouldShowRouteListViewButton() -> Bool {
        var largest = 0
        for index in Shared().favoriteRouteStops.keys {
            if index.intValue > largest {
                largest = index.intValue
            }
        }
        return largest > 2
    }
    
    func getFavState(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route) -> FavouriteRouteState {
        if (registry().hasFavouriteRouteStop(favoriteIndex: favoriteIndex.asInt32())) {
            return registry().isFavouriteRouteStop(favoriteIndex: favoriteIndex.asInt32(), stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route) ? FavouriteRouteState.usedSelf : FavouriteRouteState.usedOther
        }
        return FavouriteRouteState.notUsed
    }
    
    func FavButton(favIndex: Int) -> some View {
        Text("")
//        let state = favStates[favIndex] ?? {
//            let s = getFavState(favoriteIndex: favIndex, stopId: stopId, co: co, index: index, stop: stop, route: route)
//            DispatchQueue.main.async {
//                favStates[favIndex] = s
//            }
//            return s
//        }()
//        let handleClick: (FavouriteStopMode) -> Void = {
//            if state == FavouriteRouteState.usedSelf {
//                registry().clearFavouriteRouteStop(favoriteIndex: favIndex.asInt32(), context: appContext())
//            } else {
//                registry().setFavouriteRouteStop(favoriteIndex: favIndex.asInt32(), stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route, favouriteStopMode: $0, context: appContext())
//            }
//            favStates[favIndex] = getFavState(favoriteIndex: favIndex, stopId: stopId, co: co, index: index, stop: stop, route: route)
//        }
//        return Button(action: {}) {
//            HStack(spacing: 2) {
//                ZStack() {
//                    Circle()
//                        .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
//                        .frame(width: 30, height: 30)
//                    Text("\(favIndex)")
//                        .bold()
//                        .frame(width: 17, height: 17)
//                        .foregroundColor({
//                            switch state {
//                            case FavouriteRouteState.usedOther:
//                                return 0xFF4E4E00
//                            case FavouriteRouteState.usedSelf:
//                                return 0xFFFFFF00
//                            default:
//                                return 0xFF444444
//                            }
//                        }().asColor())
//                }
//                VStack(alignment: .leading, spacing: 0) {
//                    switch state {
//                    case FavouriteRouteState.notUsed:
//                        Text(Shared().language == "en" ? "No Route Stop Selected" : "未有設置路線巴士站")
//                            .font(.system(size: 15))
//                            .bold()
//                            .foregroundColor(0xFFB9B9B9.asColor())
//                            .lineLimit(2)
//                            .lineSpacing(0)
//                            .multilineTextAlignment(.leading)
//                            .frame(maxWidth: .infinity, alignment: .leading)
//                            .fixedSize(horizontal: false, vertical: true)
//                    case FavouriteRouteState.usedOther:
//                        let currentRoute = Shared().favoriteRouteStops[KotlinInt(int: favIndex.asInt32())]!
//                        let kmbCtbJoint = currentRoute.route.isKmbCtbJoint
//                        let coDisplay = currentRoute.co.getDisplayName(routeNumber: currentRoute.route.routeNumber, kmbCtbJoint: kmbCtbJoint, language: Shared().language, elseName: "???")
//                        let routeNumberDisplay = currentRoute.co.getDisplayRouteNumber(routeNumber: currentRoute.route.routeNumber, shortened: false)
//                        let stopName = {
//                            if (currentRoute.favouriteStopMode == FavouriteStopMode.fixed) {
//                                if (Shared().language == "en") {
//                                    return (currentRoute.co == Operator.Companion().MTR || currentRoute.co == Operator.Companion().LRT ? "" : "\(index). ") + currentRoute.stop.name.en
//                                } else {
//                                    return (currentRoute.co == Operator.Companion().MTR || currentRoute.co == Operator.Companion().LRT ? "" : "\(index). ") + currentRoute.stop.name.zh
//                                }
//                            } else {
//                                return Shared().language == "en" ? "Any" : "任何站"
//                            }
//                        }()
//                        let color = currentRoute.co.getColor(routeNumber: currentRoute.route.routeNumber, elseColor: 0xFFFFFFFF).asColor()
//                        MarqueeText(
//                            text: "\(coDisplay) \(routeNumberDisplay)",
//                            font: UIFont.systemFont(ofSize: 15),
//                            leftFade: 8,
//                            rightFade: 8,
//                            startDelay: 2,
//                            alignment: .bottomLeading
//                        )
//                            .bold()
//                            .foregroundColor(color.adjustBrightness(percentage: 0.3))
//                            .lineLimit(1)
//                            .lineSpacing(0)
//                            .multilineTextAlignment(.leading)
//                            .frame(maxWidth: .infinity, alignment: .leading)
//                        MarqueeText(
//                            text: stopName,
//                            font: UIFont.systemFont(ofSize: 15),
//                            leftFade: 8,
//                            rightFade: 8,
//                            startDelay: 2,
//                            alignment: .bottomLeading
//                        )
//                            .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.3))
//                            .lineLimit(1)
//                            .lineSpacing(0)
//                            .multilineTextAlignment(.leading)
//                            .frame(maxWidth: .infinity, alignment: .leading)
//                    case FavouriteRouteState.usedSelf:
//                        let isClosestStopMode = Shared().favoriteRouteStops[KotlinInt(int: favIndex.asInt32())]?.favouriteStopMode == FavouriteStopMode.closest
//                        Text(isClosestStopMode ? (Shared().language == "en" ? "Selected as Any Closes Stop on This Route" : "已設置為本路線最近的任何巴士站") : (Shared().language == "en" ? "Selected as This Route Stop" : "已設置為本路線巴士站"))
//                            .font(.system(size: 15))
//                            .bold()
//                            .foregroundColor((isClosestStopMode ? 0xFFFFE496 : 0xFFFFFFFF).asColor())
//                            .lineLimit(2)
//                            .lineSpacing(0)
//                            .multilineTextAlignment(.leading)
//                            .frame(maxWidth: .infinity, alignment: .leading)
//                            .fixedSize(horizontal: false, vertical: true)
//                    default:
//                        Text("")
//                    }
//                }
//            }
//            .padding(10)
//        }
//        .buttonStyle(PlainButtonStyle())
//        .background { 0xFF1A1A1A.asColor() }
//        .frame(width: 178.0, height: 47.0)
//        .clipShape(RoundedRectangle(cornerRadius: 50))
//        .simultaneousGesture(
//            LongPressGesture()
//                .onEnded { _ in
//                    playHaptics()
//                    handleClick(FavouriteStopMode.closest)
//                }
//        )
//        .highPriorityGesture(
//            TapGesture()
//                .onEnded { _ in
//                    handleClick(FavouriteStopMode.fixed)
//                }
//        )
    }

}

#Preview {
    FavView(data: nil)
}
