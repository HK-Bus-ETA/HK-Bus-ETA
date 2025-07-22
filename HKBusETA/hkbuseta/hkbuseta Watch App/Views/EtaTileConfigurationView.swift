//
//  EtaView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared

struct EtaTileConfigurationView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = StateFlowObservable(stateFlow: Shared().jointOperatedColorFractionState)
    
    @StateObject private var favouriteRouteStops = StateFlowListObservable(stateFlow: Shared().favoriteRouteStops)
    
    private let tileId: Int
    @State private var selectStates: [Int]
    @State private var selectedGroup: BilingualText
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.tileId = data["tileId"] as! Int
        self.selectStates = Tiles().getEtaTileConfiguration(tileId: tileId.asInt32()).map { Int(truncating: $0) }
        self.selectedGroup = typedValue(Shared().favoriteRouteStops).first!.name
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollViewReader { value in
                ScrollView(.vertical) {
                    VStack(alignment: .center, spacing: 1.scaled(appContext)) {
                        Spacer().frame(fixedSize: 10.scaled(appContext))
                        Text(Shared().language == "en" ? "Select Routes" : "請選擇路線")
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
                        Spacer().frame(fixedSize: 5.scaled(appContext))
                        Text(Shared().language == "en" ? "Selected Favourite Routes will display in the Tile" : "所選收藏路線將顯示在資訊方塊中")
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 12.scaled(appContext, true))
                        Text(Shared().language == "en" ? "Multiple routes may be selected if their respective stop is close by" : "可選多條巴士站相近的路線")
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 12.scaled(appContext, true))
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
                        .buttonStyle(PlainButtonStyle())
                        .background {
                            colorInt(0xFF1A1A1A)
                                .asColor()
                                .clipShape(RoundedRectangle(cornerRadius: 25))
                        }
                        Spacer().frame(fixedSize: 10.scaled(appContext))
                        let routeStops = FavouriteRouteGroupKt.getByName(favouriteRouteStops.state, name: selectedGroup)!.favouriteRouteStops
                        if routeStops.isEmpty {
                            Text(Shared().language == "en" ? "No favourite routes" : "沒有收藏路線")
                                .multilineTextAlignment(.center)
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                                .lineLimit(2)
                                .autoResizing(maxSize: 23.scaled(appContext, true))
                        } else {
                            ForEach(routeStops.withIndex(), id: \.1.favouriteId) { (index, routeStop) in
                                SelectButton(numIndex: index + 1, favouriteRouteStop: routeStop)
                                    .id(routeStop.favouriteId)
                                Spacer().frame(fixedSize: 5.scaled(appContext))
                            }
                            .animation(.default, value: favouriteRouteStops.state)
                        }
                        Spacer().frame(fixedSize: 60.scaled(appContext))
                    }
                }
            }
            ZStack(alignment: .bottom) {
                LinearGradient(gradient: Gradient(colors: [colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0x00000000).asColor()]), startPoint: .bottom, endPoint: .top)
                    .frame(height: 55.scaled(appContext))
                HStack {
                    Button(action: {
                        registryNoUpdate(appContext).setEtaTileConfiguration(tileId: tileId.asInt32(), favouriteIndexes: selectStates.map { $0.asKt() }, context: appContext)
                        appContext.finish()
                    }) {
                        Text(Shared().language == "en" ? "Confirm" : "確認選擇")
                            .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                            .foregroundColor(colorInt(!selectStates.isEmpty ? 0xFF62FF00 : 0xFF444444).asColor())
                    }
                    .disabled(selectStates.isEmpty)
                    .frame(height: 35.scaled(appContext))
                    .frame(maxWidth: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                    Button(action: {
                        registryNoUpdate(appContext).clearEtaTileConfiguration(tileId: tileId.asInt32(), context: appContext)
                        appContext.finish()
                    }) {
                        Image(systemName: "trash")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 15.scaled(appContext), height: 15.scaled(appContext))
                            .foregroundColor(colorInt(0xFFFF0000).asColor())
                    }
                    .frame(width: 35.scaled(appContext), height: 35.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                }
                .frame(width: 160.scaled(appContext), height: 35.scaled(appContext))
            }
        }
        .frame(maxHeight: /*@START_MENU_TOKEN@*/.infinity/*@END_MENU_TOKEN@*/)
        .ignoresSafeArea(.all, edges: .bottom)
        .onAppear {
            favouriteRouteStops.subscribe()
        }
        .onDisappear {
            jointOperatedColorFraction.unsubscribe()
            favouriteRouteStops.unsubscribe()
        }
    }
    
    func SelectButton(numIndex: Int, favouriteRouteStop: FavouriteRouteStop) -> some View {
        let favIndex = favouriteRouteStop.favouriteId
        let selectState: SelectMode
        if (!selectStates.isEmpty && selectStates[0] == favIndex) {
            selectState = SelectMode.primary
        } else if (selectStates.contains(Int(favIndex))) {
            selectState = SelectMode.secondary
        } else {
            selectState = SelectMode.none
        }
        let enabled = (!selectStates.isEmpty && selectStates[0].asInt32() == favIndex) || (selectStates.isEmpty || FavouriteRouteGroupKt.getFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: selectStates[0].asInt32()).map { it in
            (it.favouriteStopMode.isRequiresLocation && it.favouriteStopMode == favouriteRouteStop.favouriteStopMode || it.stop.location.distance(other: favouriteRouteStop.stop.location) <= 0.3) && !(it.route.routeNumber == favouriteRouteStop.route.routeNumber && it.co == favouriteRouteStop.co)
        } == true)
        let backgroundColor: Color
        switch selectState {
        case SelectMode.primary:
            backgroundColor = colorInt(0xFF46633A).asColor()
        case SelectMode.secondary:
            backgroundColor = colorInt(0xFF63543A).asColor()
        case SelectMode.none:
            backgroundColor = colorInt(0xFF1A1A1A).asColor()
        }
        
        return Button(action: { /* do nothing */ }) {
            HStack(alignment: .top, spacing: 0) {
                ZStack(alignment: .leading) {
                    Text("").frame(width: 38)
                    ZStack {
                        Circle()
                            .fill(enabled ? colorInt(0xFF3D3D3D).asColor() : colorInt(0xFF131313).asColor())
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                            .overlay {
                                if selectState.selected {
                                    Circle()
                                        .rotation(.degrees(-90))
                                        .stroke(colorInt(selectState == SelectMode.primary ? 0xFF4CFF00 : 0xFFFF8400).asColor().adjustBrightness(percentage: enabled ? 1.0 : 0.5), style: StrokeStyle(lineWidth: 2.scaled(appContext), lineCap: .butt))
                                }
                            }
                        if selectState.selected {
                            Image(systemName: "checkmark")
                                .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                                .frame(width: 17.scaled(appContext), height: 17.scaled(appContext))
                                .foregroundColor((selectState == SelectMode.primary ? colorInt(0xFF4CFF00).asColor() : colorInt(0xFFFF8400).asColor()).adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        } else {
                            Text("\(numIndex)")
                                .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                                .foregroundColor(colorInt(0xFFFFFF00).asColor().adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        }
                    }
                }
                .padding([.leading, .top, .bottom], 10)
                VStack(alignment: .leading, spacing: 1.scaled(appContext)) {
                    let stopName = {
                        if (favouriteRouteStop.favouriteStopMode == FavouriteStopMode.fixed) {
                            return favouriteRouteStop.stop.name
                        } else {
                            if (!selectStates.isEmpty && (selectStates.count > 1 || selectState != SelectMode.primary) && FavouriteRouteGroupKt.getFavouriteRouteStop(typedValue(Shared().favoriteRouteStops), favouriteId: selectStates[0].asInt32())?.favouriteStopMode.isRequiresLocation == true) {
                                return BilingualText(zh: "共同最近的任何站", en: "Any Common Closest")
                            } else {
                                return BilingualText(zh: "最近的任何站", en: "Any Closest")
                            }
                        }
                    }()
                    let index = favouriteRouteStop.index
                    let route = favouriteRouteStop.route
                    let kmbCtbJoint = route.isKmbCtbJoint
                    let gmbRegion = route.gmbRegion
                    let co = favouriteRouteStop.co
                    let routeNumber = route.routeNumber
                    let destName = registry(appContext).getStopSpecialDestinations(stopId: favouriteRouteStop.stopId, co: favouriteRouteStop.co, route: route, prependTo: true)
                    let color = operatorColor(favouriteRouteStop.co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }
                    let operatorName = favouriteRouteStop.co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: route.isKmbCtbJoint, gmbRegion: gmbRegion, language: Shared().language, elseName: "???")
                    let mainText = "\(operatorName) \(favouriteRouteStop.co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))"
                    let routeText = destName.get(language: Shared().language)
                    let subText = ((co.isTrain || favouriteRouteStop.favouriteStopMode == FavouriteStopMode.closest ? "" : "\(index). ") + stopName.get(language: Shared().language)).asAttributedString()
                    
                    VStack(alignment: .leading, spacing: 0) {
                        UserMarqueeText(
                            text: mainText,
                            font: UIFont.systemFont(ofSize: 19.scaled(appContext, true), weight: .bold),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .bottomLeading
                        )
                        .foregroundColor(color.asColor().adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .onAppear {
                            if kmbCtbJoint {
                                jointOperatedColorFraction.subscribe()
                            }
                        }
                        UserMarqueeText(
                            text: routeText,
                            font: UIFont.systemFont(ofSize: 17.scaled(appContext, true)),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .bottomLeading
                        )
                        .foregroundColor(.white.adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Spacer().frame(fixedSize: 3.scaled(appContext))
                        UserMarqueeText(
                            text: subText,
                            font: UIFont.systemFont(ofSize: 14.scaled(appContext, true)),
                            marqueeStartDelay: 2,
                            marqueeAlignment: .bottomLeading
                        )
                        .foregroundColor(.white.adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(5)
            }
        }
        .disabled(!enabled)
        .buttonStyle(PlainButtonStyle())
        .background { backgroundColor }
        .frame(minWidth: 178.0.scaled(appContext), maxWidth: 178.0.scaled(appContext), minHeight: 47.0.scaled(appContext))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    if enabled {
                        playHaptics()
                        if selectState == SelectMode.primary {
                            selectStates.removeAll { $0 == favIndex }
                        } else {
                            selectStates.removeAll { $0 == favIndex }
                            selectStates.insert(Int(favIndex), at: 0)
                        }
                    }
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    if enabled {
                        if selectState.selected {
                            selectStates.removeAll { $0 == favIndex }
                        } else {
                            selectStates.append(Int(favIndex))
                        }
                    }
                }
        )
    }

}

enum SelectMode {
    
    case primary
    case secondary
    case none

    var selected: Bool {
        switch self {
        case .primary, .secondary:
            return true
        case .none:
            return false
        }
    }
    
}
