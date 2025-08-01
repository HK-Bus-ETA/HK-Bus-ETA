//
//  EtaView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared

struct EtaView: AppScreenView {
    
    @StateObject private var alternateStopNamesShowingState = StateFlowObservable(stateFlow: Shared().alternateStopNamesShowingState, initSubscribe: true)

    @State private var eta: Registry.ETAQueryResult? = nil
    @State private var nextBus: Registry.NextBusPosition? = nil
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()

    let freshnessTimer = Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()

    @Environment(\.isLuminanceReduced) var ambientMode

    @State private var stopId: String
    @State private var co: Operator
    @State private var index: Int
    @State private var stop: Stop
    @State private var route: Route
    @State private var offsetStart: Int

    @State private var resolvedDestName: BilingualFormattedText
    @State private var stopList: [Registry.StopData]
    @State private var stopData: Registry.StopData?
    @State private var etaDisplayMode: ETADisplayMode
    @State private var lrtDirectionMode: Bool
    @State private var currentBranch: Route

    @State private var freshness: Bool

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
        self.stopData = stopData
        let branches = registry(appContext).getAllBranchRoutes(routeNumber: route.routeNumber, bound: route.idBound(co: co), co: co, gmbRegion: route.gmbRegion, includeFakeRoutes: false)
        let currentBranch = AppContextWatchOSKt.findMostActiveRoute(TimetableUtilsKt.currentBranchStatus(branches, time: TimeUtilsKt.currentLocalDateTime(), context: appContext, resolveSpecialRemark: false))
        self.currentBranch = currentBranch
        self.resolvedDestName = {
            if co.isTrain {
                return registry(appContext).getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true).asFormattedText(style: KotlinArray(size: 0) { _ in nil })
            } else if stopData?.branchIds.contains(currentBranch) != false {
                return route.resolvedDestWithBranchFormatted(prependTo: true, branch: currentBranch, selectedStop: index.asInt32(), selectedStopId: stopId, context: appContext, style: KotlinArray(size: 0) { _ in nil })
            } else {
                return route.resolvedDestFormatted(prependTo: true, style: KotlinArray(size: 0) { _ in nil })
            }
        }()
        self.etaDisplayMode = Shared().etaDisplayMode
        self.lrtDirectionMode = Shared().lrtDirectionMode
        self.freshness = true
    }

    var body: some View {
        VStack(alignment: .center, spacing: 3.scaled(appContext)) {
            Spacer().frame(fixedSize: 3.scaled(appContext))
            let stopName = {
                if route.isKmbCtbJoint && alternateStopNamesShowingState.state.boolValue {
                    return registry(appContext).findJointAlternateStop(stopId: stopId, routeNumber: route.routeNumber).stop.name
                } else {
                    return stop.name
                }
            }()
            Text(co.isTrain ? stopName.get(language: Shared().language) : "\(index). \(stopName.get(language: Shared().language))")
                .multilineTextAlignment(.center)
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(2)
                .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
            Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false).asAttributedString() + " ".asAttributedString() + resolvedDestName.get(language: Shared().language).asAttributedString(defaultFontSize: 12.scaled(appContext, true)))
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(1)
                .autoResizing(maxSize: 12.scaled(appContext, true))
            Spacer().frame(fixedSize: 2.scaled(appContext))
            let nextBusText = nextBus?.getDisplayText(allStops: stopList, alternateStopNames: nil, alternateStopNamesShowing: alternateStopNamesShowingState.state.boolValue && route.isKmbCtbJoint, mode: NextBusTextDisplayMode.compact, context: appContext, language: Shared().language)
            Text((co.isBus ? nextBusText : nil)?.asAttributedString(defaultFontSize: 11.scaled(appContext, true)) ?? "".asAttributedString())
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: 0.8).adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(2)
                .autoResizing(maxSize: 11.scaled(appContext, true))
            Spacer().frame(fixedSize: 2.scaled(appContext))
            ETALine(lines: eta, seq: 1)
            ETALine(lines: eta, seq: 2)
            ETALine(lines: eta, seq: 3)
            Spacer().frame(fixedSize: 3.scaled(appContext))
            if ambientMode {
                Spacer().frame(fixedSize: 25.scaled(appContext))
            } else {
                HStack(alignment: /*@START_MENU_TOKEN@*/.center/*@END_MENU_TOKEN@*/, spacing: 3.scaled(appContext)) {
                    Button(action: {
                        let data = newAppDataConatiner()
                        let newStopData = stopList[index - 2]
                        data["stopId"] = newStopData.stopId
                        data["co"] = co
                        data["index"] = index - 1
                        data["stop"] = newStopData.stop
                        data["route"] = newStopData.route
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.dummy))
                        appContext.finish()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                        }
                    }) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 17.scaled(appContext, true)))
                            .foregroundColor(index > 1 ? colorInt(0xFFFFFFFF).asColor() : colorInt(0xFF494949).asColor())
                    }
                    .frame(width: 25.scaled(appContext), height: 25.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                    .ignoresSafeArea(.all)
                    .disabled(index <= 1)

                    if co == Operator.Companion().LRT {
                        Button(action: {
                            registry(appContext).setLrtDirectionMode(lrtDirectionMode: !lrtDirectionMode, context: appContext)
                            lrtDirectionMode = Shared().lrtDirectionMode
                            appContext.showToastText(text: lrtDirectionMode ? (Shared().language == "en" ? "Display all Light Rail routes in the same direction" : "顯示所有相同方向輕鐵路線") : (Shared().language == "en" ? "Display only the select Light Rail route" : "只顯示該輕鐵路線"), duration: ToastDuration.short_)
                            let options = Registry.EtaQueryOptions(lrtDirectionMode: Shared().lrtDirectionMode, lrtAllMode: false, selectedBranch: nil)
                            fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: route, options: options) { eta = $0 }
                            if let stopData = stopData {
                                if co.isBus && stopData.branchIds.contains(currentBranch) {
                                    fetchNextBus(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: currentBranch, stopList: stopList, options: options) { nextBus = $0 }
                                } else {
                                    nextBus = nil
                                }
                            }
                        }) {
                            Image(systemName: "arrow.forward.circle")
                                .font(.system(size: 17.scaled(appContext, true)))
                                .foregroundColor(Operator.Companion().LRT.getOperatorColor(elseColor: 0xFFFFFFFF as Int64).asColor().adjustBrightness(percentage: lrtDirectionMode ? 1 : 0.4))
                        }
                        .frame(width: 25.scaled(appContext), height: 25.scaled(appContext))
                        .clipShape(RoundedRectangle(cornerRadius: 25))
                        .buttonStyle(PlainButtonStyle())
                        .background {
                            colorInt(0xFF1A1A1A)
                                .asColor()
                                .clipShape(RoundedRectangle(cornerRadius: 25))
                        }
                        .ignoresSafeArea(.all)
                    }

                    Button(action: {
                        let data = newAppDataConatiner()
                        data["stopId"] = stopId
                        data["co"] = co
                        data["index"] = index
                        data["stop"] = stop
                        data["route"] = route
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaMenu, data))
                    }) {
                        Text(Shared().language == "en" ? "More" : "更多")
                            .font(.system(size: 17.scaled(appContext, true)))
                    }
                    .frame(width: 65.scaled(appContext), height: 25.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                    .ignoresSafeArea(.all)

                    Button(action: {
                        let data = newAppDataConatiner()
                        let newStopData = stopList[index]
                        data["stopId"] = newStopData.stopId
                        data["co"] = co
                        data["index"] = index + 1
                        data["stop"] = newStopData.stop
                        data["route"] = newStopData.route
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.dummy))
                        appContext.finish()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                        }
                    }) {
                        Image(systemName: "arrow.right")
                            .font(.system(size: 17.scaled(appContext, true)))
                            .foregroundColor(index < stopList.count ? colorInt(0xFFFFFFFF).asColor() : colorInt(0xFF494949).asColor())
                    }
                    .frame(width: 25.scaled(appContext), height: 25.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                    }
                    .ignoresSafeArea(.all)
                    .disabled(index >= stopList.count)
                }
            }
        }
        .frame(height: Double(appContext.screenHeight) * 0.8)
        .onReceive(etaTimer) { _ in
            let options = Registry.EtaQueryOptions(lrtDirectionMode: Shared().lrtDirectionMode, lrtAllMode: false, selectedBranch: nil)
            fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: route, options: options) { eta = $0 }
            if let stopData = stopData {
                if co.isBus && stopData.branchIds.contains(currentBranch) {
                    fetchNextBus(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: currentBranch, stopList: stopList, options: options) { nextBus = $0 }
                } else {
                    nextBus = nil
                }
            }
        }
        .onReceive(freshnessTimer) { _ in
            freshness = eta?.isOutdated() != true
        }
        .onAppear {
            let options = Registry.EtaQueryOptions(lrtDirectionMode: Shared().lrtDirectionMode, lrtAllMode: false, selectedBranch: nil)
            fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: route, options: options) { eta = $0 }
            if let stopData = stopData {
                if co.isBus && stopData.branchIds.contains(currentBranch) {
                    fetchNextBus(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: currentBranch, stopList: stopList, options: options) { nextBus = $0 }
                } else {
                    nextBus = nil
                }
            }
        }
        .gesture(
            TapGesture()
                .onEnded { _ in
                    etaDisplayMode = etaDisplayMode.next
                    registry(appContext).setEtaDisplayMode(etaDisplayMode: etaDisplayMode, context: appContext)
                }
        )
    }

    func ETALine(lines: Registry.ETAQueryResult?, seq: Int) -> some View {
        let baseSize = lines?.nextCo == Operator.Companion().LRT && Shared().lrtDirectionMode ? 17.5 : 20.0
        let text = Shared().getResolvedText(lines, seq: seq.asInt32(), etaDisplayMode: etaDisplayMode, context: appContext).asAttributedString(defaultFontSize: max(baseSize.scaled(appContext), baseSize.scaled(appContext, seq == 1)))
        return MarqueeText(
            text: text,
            font: UIFont.systemFont(ofSize: baseSize.scaled(appContext, true)),
            startDelay: 2,
            alignment: .center
        )
        .foregroundColor(freshness ? colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: lines == nil || (ambientMode && seq > 1) ? 0.7 : 1) : colorInt(0xFFFFB0B0).asColor())
        .lineLimit(1)
        .frame(minHeight: baseSize.scaled(appContext, true))
    }

}
