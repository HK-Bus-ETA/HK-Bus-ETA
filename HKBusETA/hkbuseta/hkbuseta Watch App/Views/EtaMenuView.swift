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

struct EtaMenuView: View {
    
    @StateObject private var maxFavItems = FlowStateObservable(defaultValue: KotlinInt(int: Shared().suggestedMaxFavouriteRouteStopState), nativeFlow: Shared().suggestedMaxFavouriteRouteStopStateFlow)
    
    @State private var stopId: String
    @State private var co: Operator
    @State private var index: Int
    @State private var stop: Stop
    @State private var route: Route
    @State private var offsetStart: Int
    
    @State private var stopList: [Registry.StopData]
    @State private var favStates: [Int: FavouriteRouteState] = [:]
    
    init(data: [String: Any]?) {
        self.stopId = data?["stopId"] as! String
        let co = data?["co"] as! Operator
        self.co = co
        self.index = data?["index"] as! Int
        self.stop = data?["stop"] as! Stop
        let route = data?["route"] as! Route
        self.route = route
        self.offsetStart = data?["offsetStart"] as? Int ?? 0
        
        self.stopList = registry().getAllStops(routeNumber: route.routeNumber, bound: co == Operator.Companion().NLB ? route.nlbId : route.bound[co]!, co: co, gmbRegion: route.gmbRegion)
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                VStack(alignment: .center, spacing: 1.scaled()) {
                    VStack(alignment: .center) {
                        Text(co.isTrain ? stop.name.get(language: Shared().language) : "\(index). \(stop.name.get(language: Shared().language))")
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled())
                            .bold()
                        if (stop.remark != nil) {
                            Text(stop.remark!.get(language: Shared().language))
                                .foregroundColor(0xFFFFFFFF.asColor())
                                .lineLimit(1)
                                .autoResizing(maxSize: 13.scaled())
                        }
                        let destName = registry().getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true)
                        Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false) + " " + destName.get(language: Shared().language))
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .lineLimit(1)
                            .autoResizing(maxSize: 12.scaled())
                    }
                    Spacer(minLength: 10.scaled())
                    Text(Shared().language == "en" ? "More Info & Actions" : "更多資訊及功能")
                        .foregroundColor(0xFFFFFFFF.asColor())
                        .lineLimit(1)
                        .autoResizing(maxSize: 14.scaled())
                    Spacer(minLength: 5.scaled())
                    if stop.kmbBbiId != nil {
                        KmbBbiButton(kmbBbiId: stop.kmbBbiId!)
                    }
                    Spacer(minLength: 5.scaled())
                    SearchNearbyButton()
                    Spacer(minLength: 10.scaled())
                    Text(Shared().language == "en" ? "Set Favourite Routes" : "設置最喜愛路線")
                        .font(.system(size: 14.scaled()))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled())
                    Text(Shared().language == "en" ? "Section to set/clear this route stop from the corresponding indexed favourite route" : "以下可設置/清除對應的最喜愛路線")
                        .font(.system(size: 10.scaled()))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled())
                    Text(Shared().language == "en" ?"Route stops can be used in Tiles" : "最喜愛路線可在資訊方塊中顯示")
                        .font(.system(size: 10.scaled()))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled())
                    Spacer().frame(height: 5.scaled())
                    Text(Shared().language == "en" ? "Tap to set this stop\nLong press to set to display any closes stop of the route" : "點擊設置此站 長按設置顯示路線最近的任何站")
                        .font(.system(size: 10.scaled()))
                        .bold()
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled())
                    Spacer(minLength: 10.scaled())
                    Button(action: {
                        let target = (1...30).first {
                            let favState = getFavState(favoriteIndex: $0, stopId: stopId, co: co, index: index, stop: stop, route: route)
                            return favState == .notUsed || favState == .usedSelf
                        } ?? 30
                        value.scrollTo(target, anchor: .center)
                    }) {
                        Image(systemName: "chevron.down")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 15.scaled(), height: 15.scaled())
                            .padding(3.scaled())
                            .foregroundColor(0xFFFFB700.asColor())
                    }
                    .frame(width: 50.scaled(), height: 30.scaled())
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    Spacer(minLength: 10.scaled())
                    ForEach(1..<(Int(truncating: maxFavItems.state) + 1), id: \.self) { index in
                        FavButton(favIndex: index).id(index)
                        Spacer(minLength: 5.scaled())
                    }
                }
            }
        }
    }
    
    func KmbBbiButton(kmbBbiId: String) -> some View {
        Button(action: {
            let url = "https://app.kmb.hk/app1933/BBI/map/\(kmbBbiId).jpg"
            openUrl(link: url)
        }) {
            HStack(spacing: 2.scaled()) {
                ZStack {
                    Circle()
                        .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                        .frame(width: 30.scaled(), height: 30.scaled())
                    Image(systemName: "figure.walk")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 17.scaled(), height: 17.scaled())
                        .foregroundColor(0xFFFF0000.asColor())
                }
                Text(Shared().language == "en" ? "Open KMB BBI Layout Map" : "顯示九巴轉車站位置圖")
                    .font(.system(size: 15.scaled()))
                    .bold()
                    .foregroundColor(0xFFFFFFFF.asColor())
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled())
        }.background(
            Image("kmb_bbi_background")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 178.0.scaled(), height: 47.0.scaled())
                .brightness(-0.4)
                .clipShape(RoundedRectangle(cornerRadius: 50))
        )
        .buttonStyle(PlainButtonStyle())
        .frame(width: 178.0.scaled(), height: 47.0.scaled())
        .clipShape(RoundedRectangle(cornerRadius: 50))
    }
    
    func SearchNearbyButton() -> some View {
        Button(action: {
            let data = newAppDataConatiner()
            data["interchangeSearch"] = true
            data["lat"] = stop.location.lat
            data["lng"] = stop.location.lng
            data["exclude"] = [route.routeNumber]
            appContext().appendStack(screen: AppScreen.nearby, mutableData: data)
        }) {
            HStack(spacing: 2.scaled()) {
                ZStack {
                    Circle()
                        .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                        .frame(width: 30.scaled(), height: 30.scaled())
                    Image(systemName: "bus.doubledecker")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 17.scaled(), height: 17.scaled())
                        .foregroundColor(0xFFFFE15E.asColor())
                }
                Text(Shared().language == "en" ? "Find Nearby Interchanges" : "尋找附近轉乘路線")
                    .font(.system(size: 15.scaled()))
                    .bold()
                    .foregroundColor(0xFFFFFFFF.asColor())
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled())
        }.background(
            Image("interchange_background")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 178.0.scaled(), height: 47.0.scaled())
                .brightness(-0.4)
                .clipShape(RoundedRectangle(cornerRadius: 50))
        )
        .buttonStyle(PlainButtonStyle())
        .frame(width: 178.0.scaled(), height: 47.0.scaled())
        .clipShape(RoundedRectangle(cornerRadius: 50))
    }
    
    func getFavState(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route) -> FavouriteRouteState {
        if (registry().hasFavouriteRouteStop(favoriteIndex: favoriteIndex.asInt32())) {
            return registry().isFavouriteRouteStop(favoriteIndex: favoriteIndex.asInt32(), stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route) ? FavouriteRouteState.usedSelf : FavouriteRouteState.usedOther
        }
        return FavouriteRouteState.notUsed
    }
    
    func FavButton(favIndex: Int) -> some View {
        let state = favStates[favIndex] ?? {
            let s = getFavState(favoriteIndex: favIndex, stopId: stopId, co: co, index: index, stop: stop, route: route)
            DispatchQueue.main.async {
                favStates[favIndex] = s
            }
            return s
        }()
        let handleClick: (FavouriteStopMode) -> Void = {
            if state == FavouriteRouteState.usedSelf {
                registry().clearFavouriteRouteStop(favoriteIndex: favIndex.asInt32(), context: appContext())
            } else {
                registry().setFavouriteRouteStop(favoriteIndex: favIndex.asInt32(), stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route, favouriteStopMode: $0, context: appContext())
            }
            favStates[favIndex] = getFavState(favoriteIndex: favIndex, stopId: stopId, co: co, index: index, stop: stop, route: route)
        }
        return Button(action: {}) {
            HStack(spacing: 2.scaled()) {
                ZStack() {
                    Circle()
                        .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                        .frame(width: 30.scaled(), height: 30.scaled())
                    Text("\(favIndex)")
                        .bold()
                        .frame(width: 17.scaled(), height: 17.scaled())
                        .foregroundColor({
                            switch state {
                            case FavouriteRouteState.usedOther:
                                return 0xFF4E4E00
                            case FavouriteRouteState.usedSelf:
                                return 0xFFFFFF00
                            default:
                                return 0xFF444444
                            }
                        }().asColor())
                }
                VStack(alignment: .leading, spacing: 0) {
                    switch state {
                    case FavouriteRouteState.notUsed:
                        Text(Shared().language == "en" ? "No Route Stop Selected" : "未有設置路線巴士站")
                            .font(.system(size: 15.scaled()))
                            .bold()
                            .foregroundColor(0xFFB9B9B9.asColor())
                            .lineLimit(2)
                            .lineSpacing(0)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .fixedSize(horizontal: false, vertical: true)
                    case FavouriteRouteState.usedOther:
                        let currentRoute = Shared().favoriteRouteStops[KotlinInt(int: favIndex.asInt32())]!
                        let kmbCtbJoint = currentRoute.route.isKmbCtbJoint
                        let coDisplay = currentRoute.co.getDisplayName(routeNumber: currentRoute.route.routeNumber, kmbCtbJoint: kmbCtbJoint, language: Shared().language, elseName: "???")
                        let routeNumberDisplay = currentRoute.co.getDisplayRouteNumber(routeNumber: currentRoute.route.routeNumber, shortened: false)
                        let stopName = {
                            if (currentRoute.favouriteStopMode == FavouriteStopMode.fixed) {
                                if (Shared().language == "en") {
                                    return (currentRoute.co == Operator.Companion().MTR || currentRoute.co == Operator.Companion().LRT ? "" : "\(index). ") + currentRoute.stop.name.en
                                } else {
                                    return (currentRoute.co == Operator.Companion().MTR || currentRoute.co == Operator.Companion().LRT ? "" : "\(index). ") + currentRoute.stop.name.zh
                                }
                            } else {
                                return Shared().language == "en" ? "Any" : "任何站"
                            }
                        }()
                        let color = currentRoute.co.getColor(routeNumber: currentRoute.route.routeNumber, elseColor: 0xFFFFFFFF).asColor()
                        MarqueeText(
                            text: "\(coDisplay) \(routeNumberDisplay)",
                            font: UIFont.systemFont(ofSize: 15.scaled()),
                            leftFade: 8.scaled(),
                            rightFade: 8.scaled(),
                            startDelay: 2,
                            alignment: .bottomLeading
                        )
                            .bold()
                            .foregroundColor(color.adjustBrightness(percentage: 0.3))
                            .lineLimit(1)
                            .lineSpacing(0)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        MarqueeText(
                            text: stopName,
                            font: UIFont.systemFont(ofSize: 15.scaled()),
                            leftFade: 8.scaled(),
                            rightFade: 8.scaled(),
                            startDelay: 2,
                            alignment: .bottomLeading
                        )
                            .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.3))
                            .lineLimit(1)
                            .lineSpacing(0)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    case FavouriteRouteState.usedSelf:
                        let isClosestStopMode = Shared().favoriteRouteStops[KotlinInt(int: favIndex.asInt32())]?.favouriteStopMode == FavouriteStopMode.closest
                        Text(isClosestStopMode ? (Shared().language == "en" ? "Selected as Any Closes Stop on This Route" : "已設置為本路線最近的任何巴士站") : (Shared().language == "en" ? "Selected as This Route Stop" : "已設置為本路線巴士站"))
                            .font(.system(size: 15.scaled()))
                            .bold()
                            .foregroundColor((isClosestStopMode ? 0xFFFFE496 : 0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .lineSpacing(0)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .fixedSize(horizontal: false, vertical: true)
                    default:
                        Text("")
                    }
                }
            }
            .padding(10)
        }
        .buttonStyle(PlainButtonStyle())
        .background { 0xFF1A1A1A.asColor() }
        .frame(width: 178.0.scaled(), height: 47.0.scaled())
        .clipShape(RoundedRectangle(cornerRadius: 50))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    playHaptics()
                    handleClick(FavouriteStopMode.closest)
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    handleClick(FavouriteStopMode.fixed)
                }
        )
    }

}

#Preview {
    EtaMenuView(data: nil)
}
