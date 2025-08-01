//
//  ListStopsView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI
import shared

struct ListStopsView: AppScreenView {
    
    @StateObject private var jointOperatedColorFraction = StateFlowObservable(stateFlow: Shared().jointOperatedColorFractionState)
    
    @StateObject private var alternateStopNamesShowingState = StateFlowObservable(stateFlow: Shared().alternateStopNamesShowingState, initSubscribe: true)
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var scrollTarget: Int? = nil
    @State private var scrolled = false
    
    @State private var animationTick = 0
    let timer = Timer.publish(every: 5.5, on: .main, in: .common).autoconnect()
    
    @State private var specialAnimationTick = 0
    let specialTimer = Timer.publish(every: 3.5, on: .main, in: .common).autoconnect()
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [Int] = []
    @State private var etaResults: ETAResultsContainer<KotlinInt> = ETAResultsContainer()
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    @State private var route: RouteSearchResultEntry
    @State private var scrollToStop: String?
    @State private var stopId: String?
    @State private var stopIndex: Int?
    @State private var showEta: Bool
    
    @State private var routeNumber: String
    @State private var kmbCtbJoint: Bool
    @State private var co: Operator
    @State private var bound: String
    @State private var gmbRegion: GMBRegion?
    @State private var interchangeSearch: Bool
    @State private var origName: BilingualText
    @State private var destName: BilingualText
    @State private var resolvedDestName: BilingualFormattedText
    @State private var currentBranch: Route
    @State private var specialRoutesRemarks: [Route: (BilingualText, BilingualText)]
    @State private var specialRoutes: [BilingualText]
    @State private var stopList: [Registry.StopData]
    @State private var mtrStopsInterchange: [Registry.MTRInterchangeData]
    @State private var mtrLineSectionData: [MTRStopSectionData]
    @State private var alternateStopNames: [Registry.NearbyStopSearchResult]?
    @State private var closestIndex: Int

    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        let route = data["route"] as! RouteSearchResultEntry
        self.route = route
        self.scrollToStop = data["scrollToStop"] as? String
        let stopId = data["stopId"] as? String
        self.stopId = stopId
        let stopIndex = data["stopIndex"] as? Int
        self.stopIndex = stopIndex
        self.showEta = data["showEta"] as? Bool ?? true
        
        let routeNumber = route.route!.routeNumber
        self.routeNumber = routeNumber
        let kmbCtbJoint = route.route!.isKmbCtbJoint
        self.kmbCtbJoint = kmbCtbJoint
        let co = route.co
        self.co = co
        let bound = route.route!.idBound(co: route.co)
        self.bound = bound
        let gmbRegion = route.route!.gmbRegion
        self.gmbRegion = gmbRegion
        self.interchangeSearch = route.isInterchangeSearch
        let origName = route.route!.orig
        self.origName = origName
        let destName = route.route!.dest
        self.destName = destName
        let stopList = registry(appContext).getAllStops(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.stopList = stopList
        if co.isTrain {
            let mtrStopsInterchange = stopList.map { registry(appContext).getMtrStationInterchange(stopId: $0.stopId, lineName: routeNumber) }
            self.mtrStopsInterchange = mtrStopsInterchange
            self.mtrLineSectionData = MTRRouteMapUtilsKt.createMTRLineSectionData(co: co, color: co.getLineColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), stopList: stopList, mtrStopsInterchange: mtrStopsInterchange, isLrtCircular: route.route!.lrtCircular != nil, context: appContext)
        } else {
            self.mtrStopsInterchange = []
            self.mtrLineSectionData = []
        }
        if kmbCtbJoint {
            self.alternateStopNames = registry(appContext).findJointAlternateStops(stopIds: stopList.map { $0.stopId }, routeNumber: routeNumber)
        } else {
            self.alternateStopNames = nil
        }
        if stopId != nil && stopIndex != nil {
            self.closestIndex = stopList.enumerated()
                .filter { $0.element.stopId == stopId! }
                .map { $0.offset }
                .min { abs($0 - stopIndex!) < abs($1 - stopIndex!) }! + 1
        } else {
            self.closestIndex = -1
        }
        let branches = registry(appContext).getAllBranchRoutes(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion, includeFakeRoutes: false)
        let currentBranch = AppContextWatchOSKt.findMostActiveRoute(TimetableUtilsKt.currentBranchStatus(branches, time: TimeUtilsKt.currentLocalDateTime(), context: appContext, resolveSpecialRemark: false))
        self.currentBranch = currentBranch
        self.resolvedDestName = route.route!.resolvedDestWithBranchFormatted(prependTo: true, branch: currentBranch, style: KotlinArray(size: 0) { _ in nil })
        let specialRoutesRemarks: [Route: (BilingualText, BilingualText)] = Dictionary(
            uniqueKeysWithValues: branches.map { branch in
                (branch, (
                    branch.resolveSpecialRemark(context: appContext, labelType: .labelAllAndMain),
                    branch.resolveSpecialRemark(context: appContext, labelType: .labelAll)
                ))
            }
        )
        self.specialRoutesRemarks = specialRoutesRemarks
        self.specialRoutes = {
            if specialRoutesRemarks.count > 1 {
                return specialRoutesRemarks
                    .filter { $0.key != currentBranch }
                    .map { $0.value.0 }
            }
            return []
        }()
    }
    
    var body: some View {
        ScrollViewReader { reader in
            ScrollView(.vertical) {
                LazyVStack(spacing: 0) {
                    Spacer().frame(fixedSize: 10.scaled(appContext))
                    if alternateStopNames == nil {
                        Header()
                    } else {
                        Button(action: {
                            let newState = !alternateStopNamesShowingState.state.boolValue
                            registry(appContext).setAlternateStopNames(alternateStopName: newState, context: appContext)
                            let operatorName = (newState ? Operator.Companion().CTB : Operator.Companion().KMB).getDisplayName(routeNumber: routeNumber, kmbCtbJoint: false, gmbRegion: gmbRegion, language: Shared().language, elseName: "???")
                            let text = Shared().language == "en" ? "Displaying \(operatorName) stop names" : "顯示\(operatorName)站名"
                            appContext.showToastText(text: text, duration: ToastDuration.short_)
                        }) {
                            Header()
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    ForEach(stopList.indices, id: \.self) { index in
                        StopRow(index: index).id(index)
                        Divider()
                    }
                }
            }
            .onChange(of: scrollTarget) { scrollTarget in
                if !scrolled && scrollTarget != nil {
                    scrolled = true
                    withAnimation() {
                        reader.scrollTo(scrollTarget!, anchor: .center)
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        withAnimation() {
                            reader.scrollTo(scrollTarget!, anchor: .center)
                        }
                    }
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
        .onReceive(specialTimer) { _ in
            self.specialAnimationTick += 1
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
            if closestIndex >= 0 {
                scrollTarget = closestIndex - 1
            } else if let scrollToStop = self.scrollToStop {
                var index = stopList.firstIndex(where: { $0.stopId == scrollToStop })
                if index == nil && route.route!.isKmbCtbJoint {
                    let altStops = registry(appContext).findJointAlternateStops(stopIds: stopList.map { $0.stopId }, routeNumber: routeNumber)
                    index = altStops.firstIndex(where: { $0.stopId == scrollToStop })
                }
                if index != nil {
                    scrollTarget = index!
                }
            } else {
                if locationManager.readyForRequest {
                    locationManager.requestLocation()
                } else if !locationManager.authorizationDenied {
                    locationManager.requestPermission()
                }
            }
        }
        .onChange(of: locationManager.isLocationFetched) { _ in
            if locationManager.location != nil && closestIndex < 0 {
                let origin = locationManager.location!.toLocationResult().location!
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
                if closest!.distance <= 0.75 {
                    let target = Int(closest!.stopIndex)
                    closestIndex = target
                    scrollTarget = target - 1
                }
            }
        }
    }
    
    func Header() -> some View {
        let coColor = operatorColor(co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF as Int64), Operator.Companion().CTB.getOperatorColor(elseColor: 0xFFFFFFFF as Int64), jointOperatedColorFraction.state.floatValue) { _ in kmbCtbJoint }.asColor()
        return VStack(alignment: .center, spacing: 2.scaled(appContext)) {
            Text(co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: kmbCtbJoint, gmbRegion: gmbRegion, language: Shared().language, elseName: "???") + " " + co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))
                .foregroundColor(coColor.adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(1)
                .autoResizing(maxSize: 23.scaled(appContext, true), weight: .bold)
            Text(resolvedDestName.get(language: Shared().language).asAttributedString(defaultFontSize: 15.scaled(appContext, true)))
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .autoResizing(maxSize: 15.scaled(appContext, true))
            if !co.isTrain && !co.isFerry {
                if let it = specialRoutesRemarks[currentBranch]?.1 {
                    if !it.zh.trimmingCharacters(in: .whitespaces).isEmpty {
                        Text(it.get(language: Shared().language))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .autoResizing(maxSize: 13.scaled(appContext, true))
                    }
                }
            }
            if let journeyTime = currentBranch.journeyTime {
                let circular = currentBranch.isCircular ? (Shared().language == "en" ? " (One way)" : " (單向)") : ""
                Text(Shared().language == "en" ? "Full Journey Time: \(journeyTime.intValue) Min.\(circular)" : "全程車程: \(journeyTime.intValue)分鐘\(circular)")
                    .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                    .multilineTextAlignment(.center)
                    .lineLimit(1)
                    .autoResizing(maxSize: 13.scaled(appContext, true))
            }
            if !co.isTrain && !co.isFerry && !specialRoutes.isEmpty {
                CrossfadeText(
                    textList: specialRoutes.map { $0.get(language: Shared().language).asAttributedString() },
                    state: specialAnimationTick
                )
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: 0.65).adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(height: 32.scaled(appContext, true), alignment: .top)
                .autoResizing(maxSize: 13.scaled(appContext, true))
            }
        }
        .padding([.bottom], 8.scaled(appContext, true))
    }
    
    func StopRow(index: Int) -> some View {
        let stopData = stopList[index]
        let stopNumber = index + 1
        let isClosest = closestIndex == stopNumber
        let brightness = stopData.branchIds.contains(currentBranch) ? 1 : 0.65
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
            HStack(alignment: .center, spacing: 1.scaled(appContext)) {
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
                UserMarqueeText(
                    text: (alternateStopNamesShowingState.state.boolValue && alternateStopNames != nil ? alternateStopNames![index].stop : stopData.stop).remarkedName.get(language: Shared().language).asAttributedString(defaultFontSize: 18.scaled(appContext, true), defaultWeight: isClosest ? .bold : .regular),
                    font: UIFont.systemFont(ofSize: 18.scaled(appContext, true), weight: isClosest ? .bold : .regular),
                    marqueeStartDelay: 2,
                    marqueeAlignment: .bottomLeading
                )
                .foregroundColor(color)
                .frame(maxWidth: .infinity, alignment: .leading)
                if showEta {
                    ListStopETAView(appContext: appContext, etaState: etaResults.getState(key: index.asKt()))
                }
            }.contentShape(Rectangle())
        }
        .frame(width: 170.scaled(appContext))
        .frame(minHeight: 44.scaled(appContext, true))
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
                                return text1.replace("\\s+", "\n").asAttributedString(fontSize: 15.scaled(appContext, true))
                            } else {
                                let shortText = eta.firstLine.shortText
                                let text1 = shortText.first
                                let text2 = "\n" + shortText.second
                                return text1.asAttributedString(fontSize: 17.scaled(appContext, true)) + text2.asAttributedString(fontSize: 8.scaled(appContext, true))
                            }
                        }()
                        Text(text)
                            .multilineTextAlignment(.trailing)
                            .lineSpacing(0)
                            .frame(alignment: .trailing)
                            .foregroundColor(colorInt(0xFF92C6F0).asColor())
                    }
                }
            }
        }
        .frame(minWidth: 27.scaled(appContext, true), alignment: .trailing)
        .onAppear {
            etaState.subscribe()
        }
        .onDisappear {
            etaState.unsubscribe()
        }
    }
    
}
