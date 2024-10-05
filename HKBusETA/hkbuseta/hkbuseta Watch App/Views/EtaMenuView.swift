//
//  EtaView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared

struct EtaMenuView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = StateFlowObservable(stateFlow: Shared().jointOperatedColorFractionState)
    
    @StateObject private var favouriteRouteStops = StateFlowListObservable(stateFlow: Shared().favoriteRouteStops)
    
    @State private var stopId: String
    @State private var co: Operator
    @State private var index: Int
    @State private var stop: Stop
    @State private var route: Route
    @State private var resolvedDestName: BilingualText
    @State private var offsetStart: Int
    
    @State private var stopList: [Registry.StopData]
    
    @State private var selectedGroup: BilingualText
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        let stopId = data["stopId"] as! String
        self.stopId = stopId
        let co = data["co"] as! Operator
        self.co = co
        let index = data["index"] as! Int
        self.index = index
        self.stop = data["stop"] as! Stop
        let route = data["route"] as! Route
        self.route = route
        self.offsetStart = data["offsetStart"] as? Int ?? 0
        let stopList = registry(appContext).getAllStops(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion)
        self.stopList = stopList
        let stopData = stopList.enumerated().filter { $0.element.stopId == stopId }.min(by: { abs($0.offset - index) < abs($1.offset - index) })?.element
        let branches = registry(appContext).getAllBranchRoutes(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion)
        let currentBranch = AppContextWatchOSKt.findMostActiveRoute(TimetableUtilsKt.currentBranchStatus(branches, time: TimeUtilsKt.currentLocalDateTime(), context: appContext, resolveSpecialRemark: false))
        self.resolvedDestName = {
            if co.isTrain {
                return registry(appContext).getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true)
            } else if stopData?.branchIds.contains(currentBranch) != false {
                return route.resolvedDestWithBranch(prependTo: true, branch: currentBranch)
            } else {
                return route.resolvedDest(prependTo: true)
            }
        }()
        self.offsetStart = data["offsetStart"] as? Int ?? 0
        self.stopList = registry(appContext).getAllStops(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion)
        self.selectedGroup = typedValue(Shared().favoriteRouteStops).first!.name
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                VStack(alignment: .center, spacing: 1.scaled(appContext)) {
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    VStack(alignment: .center) {
                        Text(co.isTrain ? stop.name.get(language: Shared().language) : "\(index). \(stop.name.get(language: Shared().language))")
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
                        if (stop.remark != nil) {
                            Text(stop.remark!.get(language: Shared().language))
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                                .lineLimit(1)
                                .autoResizing(maxSize: 13.scaled(appContext, true))
                        }
                        Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false) + " " + resolvedDestName.get(language: Shared().language))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(1)
                            .autoResizing(maxSize: 12.scaled(appContext, true))
                    }
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    Text(Shared().language == "en" ? "More Info & Actions" : "更多資訊及功能")
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        .lineLimit(1)
                        .autoResizing(maxSize: 14.scaled(appContext, true))
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    if stop.kmbBbiId != nil {
                        KmbBbiButton(kmbBbiId: stop.kmbBbiId!)
                    }
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    SearchNearbyButton()
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    OpenOnMapsButton(stopName: stop.name, lat: stop.location.lat, lng: stop.location.lng)
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    Text(Shared().language == "en" ? "Set Favourite Routes" : "設置最喜愛路線")
                        .font(.system(size: 14.scaled(appContext, true)))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled(appContext))
                    Text(Shared().language == "en" ? "Route stops can be used in Tiles" : "最喜愛路線可在資訊方塊中顯示")
                        .font(.system(size: 10.scaled(appContext, true)))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20.scaled(appContext))
                    Spacer().frame(height: 5.scaled(appContext))
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    Button(action: {
                        let current = selectedGroup
                        let index = favouriteRouteStops.state.firstIndex { $0.name == current }!
                        selectedGroup = index + 1 >= favouriteRouteStops.state.count ? favouriteRouteStops.state.first!.name : favouriteRouteStops.state[index + 1].name
                    }) {
                        Text(selectedGroup.get(language: Shared().language))
                            .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                    }
                    .frame(width: 160.scaled(appContext), height: 35.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    AddThisStopFavButton()
                    Spacer().frame(fixedSize: 5.scaled(appContext))
                    AddAnyStopFavButton()
                    Spacer().frame(fixedSize: 10.scaled(appContext))
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
    }
    
    func OpenOnMapsButton(stopName: BilingualText, lat: Double, lng: Double) -> some View {
        Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        Image(systemName: "map")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                            .foregroundColor(colorInt(0xFF4CFF00).asColor())
                    }
                }.frame(maxHeight: .infinity)
                Text(Shared().language == "en" ? "Open Stop Location on Maps" : "在地圖上顯示巴士站位置")
                    .font(.system(size: 14.5.scaled(appContext, true), weight: .bold))
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled(appContext))
            .background(
                Image("open_map_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
                    .brightness(-0.4)
                    .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
            )
        }
        .buttonStyle(PlainButtonStyle())
        .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    appContext.handleOpenMaps(lat: lat, lng: lng, label: stopName.get(language: Shared().language), longClick: true, haptics: hapticsFeedback())()
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    appContext.handleOpenMaps(lat: lat, lng: lng, label: stopName.get(language: Shared().language), longClick: false, haptics: hapticsFeedback())()
                }
        )
    }
    
    func KmbBbiButton(kmbBbiId: String) -> some View {
        Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        Image(systemName: "figure.walk")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                            .foregroundColor(colorInt(0xFFFF0000).asColor())
                    }
                }.frame(maxHeight: .infinity)
                Text(Shared().language == "en" ? "Open KMB BBI Layout Map" : "顯示九巴轉車站位置圖")
                    .font(.system(size: 14.5.scaled(appContext, true), weight: .bold))
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled(appContext))
            .background(
                Image("kmb_bbi_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
                    .brightness(-0.4)
                    .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
            )
        }
        .buttonStyle(PlainButtonStyle())
        .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    let url = "https://app.kmb.hk/app1933/BBI/map/\(kmbBbiId).jpg"
                    appContext.handleWebImages(url: url, longClick: true, haptics: hapticsFeedback())()
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    let url = "https://app.kmb.hk/app1933/BBI/map/\(kmbBbiId).jpg"
                    appContext.handleWebImages(url: url, longClick: false, haptics: hapticsFeedback())()
                }
        )
    }
    
    func SearchNearbyButton() -> some View {
        Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        Image(systemName: "bus.doubledecker")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                            .foregroundColor(colorInt(0xFFFFE15E).asColor())
                    }
                }.frame(maxHeight: .infinity)
                Text(Shared().language == "en" ? "Find Nearby Interchanges" : "尋找附近轉乘路線")
                    .font(.system(size: 14.5.scaled(appContext, true), weight: .bold))
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled(appContext))
            .background(
                Image("interchange_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
                    .brightness(-0.4)
                    .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
            )
        }
        .buttonStyle(PlainButtonStyle())
        .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    playHaptics()
                    let data = newAppDataConatiner()
                    data["interchangeSearch"] = true
                    data["lat"] = stop.location.lat
                    data["lng"] = stop.location.lng
                    data["exclude"] = [route.routeNumber]
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.nearby, data))
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    let data = newAppDataConatiner()
                    data["interchangeSearch"] = true
                    data["lat"] = stop.location.lat
                    data["lng"] = stop.location.lng
                    data["exclude"] = [route.routeNumber]
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.nearby, data))
                }
        )
    }
    
    func AddThisStopFavButton() -> some View {
        let same = FavouriteRouteGroupKt.getByName(favouriteRouteStops.state, name: selectedGroup)!.findSame(stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route)
        let alreadySet = same.contains { $0.favouriteStopMode == FavouriteStopMode.fixed }
        return Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        Image(systemName: "star.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                            .foregroundColor(colorInt(0xFFFFFF00).asColor())
                    }
                }.frame(maxHeight: .infinity)
                Text(alreadySet ? (Shared().language == "en" ? "Added This Route Stop" : "已設置本路線巴士站") : (Shared().language == "en" ? "Add This Route Stop" : "設置本路線巴士站"))
                    .font(.system(size: 14.5.scaled(appContext, true), weight: .bold))
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: alreadySet ? 0.5 : 1))
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled(appContext))
        }
        .disabled(alreadySet)
        .buttonStyle(PlainButtonStyle())
        .background { colorInt(0xFF1A1A1A).asColor() }
        .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if (!alreadySet) {
                        let fav = FavouriteRouteStop(stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route, favouriteStopMode: FavouriteStopMode.fixed)
                        var updated = typedValue(Shared().favoriteRouteStops).map { $0 }
                        let groupIndex = Int(FavouriteRouteGroupKt.indexOfName(updated, name: selectedGroup))
                        updated[groupIndex] = updated[groupIndex].add(favouriteRouteStop: fav)
                        registry(appContext).setFavouriteRouteGroups(favouriteRouteStops: updated, context: appContext)
                    }
                }
        )
    }
    
    func AddAnyStopFavButton() -> some View {
        let same = FavouriteRouteGroupKt.getByName(favouriteRouteStops.state, name: selectedGroup)!.findSame(stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route)
        let alreadySet = same.contains { $0.favouriteStopMode == FavouriteStopMode.closest }
        return Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        Image(systemName: "star.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                            .foregroundColor(colorInt(0xFFFFFF00).asColor())
                    }
                }.frame(maxHeight: .infinity)
                Text(alreadySet ? (Shared().language == "en" ? "Added Any Closest Stop on This Route" : "已設置本路線最近的任何巴士站") : (Shared().language == "en" ? "Add Any Closes Stop on Route" : "設置本路線最近的任何巴士站"))
                    .font(.system(size: 14.5.scaled(appContext, true), weight: .bold))
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: alreadySet ? 0.5 : 1))
                    .lineLimit(2)
                    .lineSpacing(0)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10.scaled(appContext))
        }
        .disabled(alreadySet)
        .buttonStyle(PlainButtonStyle())
        .background { colorInt(0xFF1A1A1A).asColor() }
        .frame(width: 178.0.scaled(appContext), height: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if (!alreadySet) {
                        let fav = FavouriteRouteStop(stopId: stopId, co: co, index: index.asInt32(), stop: stop, route: route, favouriteStopMode: FavouriteStopMode.closest)
                        var updated = typedValue(Shared().favoriteRouteStops).map { $0 }
                        let groupIndex = Int(FavouriteRouteGroupKt.indexOfName(updated, name: selectedGroup))
                        updated[groupIndex] = updated[groupIndex].remove(favouriteRouteStop: fav).add(favouriteRouteStop: fav)
                        registry(appContext).setFavouriteRouteGroups(favouriteRouteStops: updated, context: appContext)
                    }
                }
        )
    }
    
}
