//
//  ListStopsView.swift
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

struct ListStopsView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = FlowStateObservable(defaultValue: KotlinFloat(float: Shared().jointOperatedColorFractionState), nativeFlow: Shared().jointOperatedColorFractionStateFlow)
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var scrollTarget: Int? = nil
    @State private var scrolled = false
    
    @State private var animationTick = 0
    
    let timer = Timer.publish(every: 5.5, on: .main, in: .common).autoconnect()
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [Int] = []
    @State private var etaResults: ETAResultsContainer<KotlinInt> = ETAResultsContainer()
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    @State private var route: RouteSearchResultEntry
    @State private var scrollToStop: String?
    @State private var showEta: Bool
    @State private var isAlightReminder: Bool
    
    @State private var routeNumber: String
    @State private var kmbCtbJoint: Bool
    @State private var co: Operator
    @State private var bound: String
    @State private var gmbRegion: GMBRegion?
    @State private var interchangeSearch: Bool
    @State private var origName: BilingualText
    @State private var destName: BilingualText
    @State private var resolvedDestName: BilingualText
    @State private var specialOrigs: [BilingualText]
    @State private var specialDests: [BilingualText]
    @State private var stopList: [Registry.StopData]
    @State private var lowestServiceType: Int32
    @State private var mtrStopsInterchange: [Registry.MTRInterchangeData]
    @State private var mtrLineSectionData: [MTRStopSectionData]
    @State private var closestIndex: Int
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        let route = data["route"] as! RouteSearchResultEntry
        self.route = route
        self.scrollToStop = data["scrollToStop"] as? String
        self.showEta = data["showEta"] as? Bool ?? true
        self.isAlightReminder = data["isAlightReminder"] as? Bool ?? false
        
        let routeNumber = route.route!.routeNumber
        self.routeNumber = routeNumber
        self.kmbCtbJoint = route.route!.isKmbCtbJoint
        let co = route.co
        self.co = co
        let bound = co == Operator.Companion().NLB ? route.route!.nlbId : route.route!.bound[co]!
        self.bound = bound
        let gmbRegion = route.route!.gmbRegion
        self.gmbRegion = gmbRegion
        self.interchangeSearch = route.isInterchangeSearch
        let origName = route.route!.orig
        self.origName = origName
        let destName = route.route!.dest
        self.destName = destName
        self.resolvedDestName = route.route!.resolvedDest(prependTo: true)
        let specialOrigsDests = registry(appContext).getAllOriginsAndDestinations(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.specialOrigs = specialOrigsDests.first!.map { $0 as! BilingualText }.filter { !$0.zh.eitherContains(other: origName.zh) }
        self.specialDests = specialOrigsDests.second!.map { $0 as! BilingualText }.filter { !$0.zh.eitherContains(other: destName.zh) }
        let stopList = registry(appContext).getAllStops(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.stopList = stopList
        self.lowestServiceType = stopList.min { $0.serviceType < $1.serviceType }!.serviceType
        if co.isTrain {
            let mtrStopsInterchange = stopList.map { registry(appContext).getMtrStationInterchange(stopId: $0.stopId, lineName: routeNumber) }
            self.mtrStopsInterchange = mtrStopsInterchange
            self.mtrLineSectionData = MTRRouteMapUtilsKt.createMTRLineSectionData(co: co, color: co.getLineColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), stopList: stopList, mtrStopsInterchange: mtrStopsInterchange, isLrtCircular: route.route!.lrtCircular != nil, context: appContext)
        } else {
            self.mtrStopsInterchange = []
            self.mtrLineSectionData = []
        }
        self.closestIndex = 0
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                LazyVStack(spacing: 0) {
                    let coColor = operatorColor(co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }.asColor()
                    Spacer(minLength: 10.scaled(appContext))
                    VStack(alignment: .center, spacing: 2.scaled(appContext)) {
                        Text(co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: kmbCtbJoint, language: Shared().language, elseName: "???") + " " + co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))
                            .foregroundColor(coColor.adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                            .lineLimit(1)
                            .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
                        Text(resolvedDestName.get(language: Shared().language))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .autoResizing(maxSize: 12.scaled(appContext, true))
                        if !specialOrigs.isEmpty {
                            Text(Shared().language == "en" ? ("Special From " + specialOrigs.map { $0.en }.joined(separator: "/")) : ("特別班 從" + specialOrigs.map { $0.zh }.joined(separator: "/") + "開出"))
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: 0.65).adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .autoResizing(maxSize: 12.scaled(appContext, true))
                        }
                        if !specialDests.isEmpty {
                            Text(Shared().language == "en" ? ("Special To " + specialDests.map { $0.en }.joined(separator: "/")) : ("特別班 往" + specialDests.map { $0.zh }.joined(separator: "/")))
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: 0.65).adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .autoResizing(maxSize: 12.scaled(appContext, true))
                        }
                    }
                    ForEach(stopList.indices, id: \.self) { index in
                        StopRow(index: index).id(index)
                        Divider()
                    }
                }
            }
            .onChange(of: scrollTarget) { _ in
                if !scrolled && scrollTarget != nil {
                    value.scrollTo(scrollTarget!, anchor: .center)
                    scrolled = true
                }
            }
        }
        .onAppear {
            if kmbCtbJoint {
                jointOperatedColorFraction.subscribe()
            }
        }
        .onDisappear {
            jointOperatedColorFraction.unsubscribe()
        }
        .onReceive(timer) { _ in
            self.animationTick += 1
        }
        .onReceive(etaTimer) { _ in
            if showEta {
                for index in etaActive {
                    fetchEta(appContext: appContext, stopId: stopList[index].stopId, stopIndex: index, co: co, route: route.route!) { etaResults.set(key: index.asKt(), result: $0) }
                }
            }
        }
        .onChange(of: locationManager.readyForRequest) { _ in
            if !scrolled {
                locationManager.requestLocation()
            }
        }
        .onAppear {
            if scrollToStop == nil {
                if locationManager.readyForRequest {
                    locationManager.requestLocation()
                } else if !locationManager.authorizationDenied {
                    locationManager.requestPermission()
                }
            } else {
                let index = stopList.firstIndex(where: { $0.stopId == scrollToStop! })
                if index != nil {
                    scrollTarget = index!
                }
            }
        }
        .onChange(of: locationManager.isLocationFetched) { _ in
            if locationManager.location != nil {
                let origin = locationManager.location!.coordinate.toLocationResult().location!
                let closest = stopList.indices.map { index in
                    let entry = stopList[index]
                    let stop = entry.stop
                    let location = stop.location
                    let stopStr = stop.name.get(language: Shared().language)
                    return StopEntry(stopIndex: (index + 1).asInt32(), stopName: stopStr, stopData: entry, lat: location.lat, lng: location.lng, distance: kotlinMaxDouble)
                }.map {
                    $0.distance = origin.distance(other: $0)
                    return $0
                }.min(by: {
                    $0.distance < $1.distance
                })
                if closest!.distance <= 0.3 {
                    let target = Int(closest!.stopIndex)
                    closestIndex = target
                    scrollTarget = target - 1
                }
            }
        }
    }
    
    func StopRow(index: Int) -> some View {
        let stopData = stopList[index]
        let stopNumber = index + 1
        let isClosest = closestIndex == stopNumber
        let brightness = stopData.serviceType == lowestServiceType ? 1 : 0.65
        let coColor = operatorColor(co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }.asColor()
        let color = (isClosest ? coColor : Color.white).adjustBrightness(percentage: brightness)
        return Button(action: {
            let data = newAppDataConatiner()
            data["stopId"] = stopData.stopId
            data["co"] = co
            data["index"] = stopNumber
            data["stop"] = stopData.stop
            data["route"] = stopData.route
            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
        }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                if co.isTrain && !mtrLineSectionData.isEmpty {
                    let width: CGFloat = (Set(stopList.map { $0.serviceType }).count > 1 || mtrStopsInterchange.contains(where: { !$0.outOfStationLines.isEmpty }) ? 64 : 47).dynamicSize()
                    MTRLineSection(appContext: appContext, sectionData: mtrLineSectionData[index], ambientMode: ambientMode)
                        .frame(minWidth: width, maxWidth: width, maxHeight: .infinity)
                } else {
                    Text("\(stopNumber).")
                        .frame(width: 37.scaled(appContext), alignment: .leading)
                        .font(.system(size: 18.scaled(appContext, true), weight: isClosest ? .bold : .regular))
                        .foregroundColor(color)
                }
                MarqueeText(
                    text: stopData.stop.remarkedName.get(language: Shared().language).asAttributedString(defaultFontSize: 18.scaled(appContext, true), defaultWeight: isClosest ? .bold : .regular),
                    font: UIFont.systemFont(ofSize: 18.scaled(appContext, true), weight: isClosest ? .bold : .regular),
                    startDelay: 2,
                    alignment: .bottomLeading
                )
                .foregroundColor(color)
                .frame(maxWidth: .infinity, alignment: .leading)
                if showEta {
                    ListStopETAView(appContext: appContext, etaState: etaResults.getState(key: index.asKt()))
                }
            }.contentShape(Rectangle())
        }
        .frame(width: 170.scaled(appContext), height: 40.scaled(appContext, true))
        .buttonStyle(PlainButtonStyle())
        .onAppear {
            if showEta {
                etaActive.append(index)
                fetchEta(appContext: appContext, stopId: stopData.stopId, stopIndex: index, co: co, route: route.route!) { etaResults.set(key: index.asKt(), result: $0) }
            }
        }
        .onDisappear {
            etaActive.removeAll(where: { $0 == index })
        }
    }
}

struct ListStopETAView: View {
    
    @StateObject private var etaState: FlowStateObservable<Registry.ETAQueryResult?>
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, etaState: ETAResultsState) {
        self.appContext = appContext
        self._etaState = StateObject(wrappedValue: FlowStateObservable(defaultValue: etaState.state, nativeFlow: etaState.stateFlow))
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
                        let shortText = eta.firstLine.shortText
                        let text1 = shortText.first
                        let text2 = "\n" + shortText.second
                        let text = text1.asAttributedString(fontSize: 17.scaled(appContext, true)) + text2.asAttributedString(fontSize: 8.scaled(appContext, true))
                        Text(text)
                            .multilineTextAlignment(.trailing)
                            .lineSpacing(0)
                            .frame(alignment: .trailing)
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                            .lineLimit(2)
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
