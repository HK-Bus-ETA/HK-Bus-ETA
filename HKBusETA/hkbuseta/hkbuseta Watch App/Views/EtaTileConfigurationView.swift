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

struct EtaTileConfigurationView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = FlowStateObservable(defaultValue: KotlinFloat(float: Shared().jointOperatedColorFractionState), nativeFlow: Shared().jointOperatedColorFractionStateFlow)
    
    @StateObject private var maxFavItems = FlowStateObservable(defaultValue: KotlinInt(int: Shared().suggestedMaxFavouriteRouteStopState), nativeFlow: Shared().suggestedMaxFavouriteRouteStopStateFlow)
    
    private let tileId: Int
    @State private var selectStates: [Int]
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.tileId = data["tileId"] as! Int
        self.selectStates = Tiles().getEtaTileConfiguration(tileId: tileId.asInt32()).map { Int(truncating: $0) }
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
                        Text(Shared().language == "en" ? "Selected Favourite Routes will display in the Tile" : "所選最喜愛路線將顯示在資訊方塊中")
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
                        if Shared().favoriteRouteStops.isEmpty {
                            Text(Shared().language == "en" ? "No favourite routes" : "沒有最喜愛路線")
                                .multilineTextAlignment(.center)
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                                .lineLimit(2)
                                .autoResizing(maxSize: 23.scaled(appContext, true))
                        } else {
                            ForEach(1..<(Int(truncating: maxFavItems.state) + 1), id: \.self) { index in
                                if Shared().favoriteRouteStops[index.asKt()] != nil {
                                    SelectButton(favIndex: index)
                                    Spacer().frame(fixedSize: 5.scaled(appContext))
                                }
                            }
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
                    .clipShape(RoundedRectangle(cornerRadius: 25))
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
                }
                .frame(width: 160.scaled(appContext), height: 35.scaled(appContext))
            }
        }
        .frame(maxHeight: /*@START_MENU_TOKEN@*/.infinity/*@END_MENU_TOKEN@*/)
        .ignoresSafeArea(.all, edges: .bottom)
        .onAppear {
            maxFavItems.subscribe()
        }
        .onDisappear {
            jointOperatedColorFraction.unsubscribe()
            maxFavItems.unsubscribe()
        }
    }
    
    func SelectButton(favIndex: Int) -> some View {
        let favouriteStopRoute = Shared().favoriteRouteStops[favIndex.asKt()]
        let selectState: SelectMode
        if (!selectStates.isEmpty && selectStates[0] == favIndex) {
            selectState = SelectMode.primary
        } else if (selectStates.contains(favIndex)) {
            selectState = SelectMode.secondary
        } else {
            selectState = SelectMode.none
        }
        let enabled = (!selectStates.isEmpty && selectStates[0] == favIndex) || (favouriteStopRoute != nil && (selectStates.isEmpty || Shared().favoriteRouteStops[selectStates[0].asKt()].map { it in
            (it.favouriteStopMode.isRequiresLocation && it.favouriteStopMode == favouriteStopRoute!.favouriteStopMode || it.stop.location.distance(other: favouriteStopRoute!.stop.location) <= 0.3) && !(it.route.routeNumber == favouriteStopRoute!.route.routeNumber && it.co == favouriteStopRoute!.co)
        } == true))
        let backgroundColor: Color
        switch selectState {
        case SelectMode.primary:
            backgroundColor = colorInt(0xFF46633A).asColor()
        case SelectMode.secondary:
            backgroundColor = colorInt(0xFF63543A).asColor()
        case SelectMode.none:
            backgroundColor = colorInt(0xFF1A1A1A).asColor()
        }
        
        return Button(action: {}) {
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
                            Text("\(favIndex)")
                                .font(.system(size: 17.scaled(appContext, true), weight: .bold))
                                .foregroundColor((favouriteStopRoute != nil ? colorInt(0xFFFFFF00).asColor() : colorInt(0xFF444444).asColor()).adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                        }
                    }
                }
                .padding(10)
                VStack(alignment: .leading, spacing: 1.scaled(appContext)) {
                    if favouriteStopRoute == nil {
                        HStack(alignment: .center) {
                            Text(Shared().language == "en" ? "No Route Selected" : "未有設置路線")
                                .font(.system(size: 16.scaled(appContext, true), weight: .bold))
                                .foregroundColor(colorInt(0xFF505050).asColor())
                                .lineLimit(2)
                                .lineSpacing(0)
                                .multilineTextAlignment(.leading)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .fixedSize(horizontal: false, vertical: true)
                        }.frame(maxHeight: .infinity)
                    } else {
                        let stopName = {
                            if (favouriteStopRoute!.favouriteStopMode == FavouriteStopMode.fixed) {
                                return favouriteStopRoute!.stop.name
                            } else {
                                if (!selectStates.isEmpty && (selectStates.count > 1 || selectState != SelectMode.primary) && Shared().favoriteRouteStops[selectStates[0].asKt()]?.favouriteStopMode.isRequiresLocation == true) {
                                    return BilingualText(zh: "共同最近的任何站", en: "Any Common Closes")
                                } else {
                                    return BilingualText(zh: "最近的任何站", en: "Any Closes")
                                }
                            }
                        }()
                        let index = favouriteStopRoute!.index
                        let route = favouriteStopRoute!.route
                        let kmbCtbJoint = route.isKmbCtbJoint
                        let co = favouriteStopRoute!.co
                        let routeNumber = route.routeNumber
                        let destName = registry(appContext).getStopSpecialDestinations(stopId: favouriteStopRoute!.stopId, co: favouriteStopRoute!.co, route: route, prependTo: true)
                        let color = operatorColor(favouriteStopRoute!.co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }
                        let operatorName = favouriteStopRoute!.co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: route.isKmbCtbJoint, language: Shared().language, elseName: "???")
                        let mainText = "\(operatorName) \(favouriteStopRoute!.co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))"
                        let routeText = destName.get(language: Shared().language)
                        let subText = ((co.isTrain || favouriteStopRoute!.favouriteStopMode == FavouriteStopMode.closest ? "" : "\(index). ") + stopName.get(language: Shared().language)).asAttributedString()
                        
                        VStack(alignment: .leading, spacing: 0) {
                            MarqueeText(
                                text: mainText,
                                font: UIFont.systemFont(ofSize: 19.scaled(appContext, true), weight: .bold),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(color.asColor().adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                            .lineLimit(1)
                            .onAppear {
                                if kmbCtbJoint {
                                    jointOperatedColorFraction.subscribe()
                                }
                            }
                            MarqueeText(
                                text: routeText,
                                font: UIFont.systemFont(ofSize: 17.scaled(appContext, true)),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(.white.adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                            .lineLimit(1)
                            Spacer().frame(fixedSize: 3.scaled(appContext))
                            MarqueeText(
                                text: subText,
                                font: UIFont.systemFont(ofSize: 14.scaled(appContext, true)),
                                startDelay: 2,
                                alignment: .bottomLeading
                            )
                            .foregroundColor(.white.adjustBrightness(percentage: enabled ? 1.0 : 0.5))
                            .lineLimit(1)
                        }
                    }
                }.padding(.vertical, 5)
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
                            selectStates.insert(favIndex, at: 0)
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
                            selectStates.append(favIndex)
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
