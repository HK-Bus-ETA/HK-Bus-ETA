//
//  ListRouteView.swift
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

struct ListRoutesView: View {
    
    @State private var animationTick = 0
    
    let timer = Timer.publish(every: 5.5, on: .main, in: .common).autoconnect()
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [String: StopIndexedRouteSearchResultEntry] = [:]
    @State private var etaResults: [String: Registry.ETAQueryResult?] = [:]
    
    @Environment(\.isLuminanceReduced) var isLuminanceReduced
    @State var ambientMode = false
    
    @State var result: [StopIndexedRouteSearchResultEntry]
    @State var listType: RouteListType
    @State var showEta: Bool
    @State var recentSort: RecentSortMode
    @State var proximitySortOrigin: Coordinates?
    @State var allowAmbient: Bool
    
    @State var activeSortMode: RouteSortMode
    @State var sortedByMode: [RouteSortMode: [StopIndexedRouteSearchResultEntry]]
    @State var sortedResults: [StopIndexedRouteSearchResultEntry]
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        let rawResult = data["result"]! as! [Any]
        var casedResult: [StopIndexedRouteSearchResultEntry]
        if rawResult.isEmpty {
            casedResult = []
        } else if rawResult.first is StopIndexedRouteSearchResultEntry {
            casedResult = rawResult as! [StopIndexedRouteSearchResultEntry]
        } else {
            let a = rawResult as! [RouteSearchResultEntry]
            casedResult = a.map { StopIndexedRouteSearchResultEntry.Companion().fromRouteSearchResultEntry(resultEntry: $0) }
        }
        casedResult.removeAll(where: {
            if $0.route == nil {
                let route = registry().findRouteByKey(lookupKey: $0.routeKey, routeNumber: nil)
                if route == nil {
                    return true
                } else {
                    $0.route = route
                }
            }
            if $0.stopInfo != nil && $0.stopInfo!.data == nil {
                let stop = registry().getStopById(stopId: $0.stopInfo!.stopId)
                if stop == nil {
                    return true
                } else {
                    $0.stopInfo!.data = stop
                }
            }
            return false
        })
        casedResult.forEach {
            let route = $0.route
            let co = $0.co
            let stopInfo = $0.stopInfo
            if route != nil && stopInfo != nil {
                $0.stopInfoIndex = registry().getAllStops(routeNumber: route!.routeNumber, bound: route!.bound[co]!, co: co, gmbRegion: route!.gmbRegion).firstIndex(where: {
                    $0.stopId == stopInfo!.stopId
                })?.asInt32() ?? -1
            }
        }
        self.result = casedResult
        let listType = data["listType"] as? RouteListType ?? RouteListType.Companion().NORMAL
        self.listType = listType
        self.showEta = data["showEta"] as? Bool ?? false
        let recentSort = data["recentSort"] as? RecentSortMode ?? RecentSortMode.disabled
        self.recentSort = recentSort
        let proximitySortOrigin = data["proximitySortOrigin"] as? Coordinates
        self.proximitySortOrigin = proximitySortOrigin
        self.allowAmbient = data["allowAmbient"] as? Bool ?? false
        let activeSortMode = recentSort.forcedMode ? recentSort.defaultSortMode : {
            let preferred = Shared().routeSortModePreference[listType]
            if preferred == nil {
                return RouteSortMode.normal
            } else {
                if preferred!.isLegalMode(allowRecentSort: recentSort == RecentSortMode.choice, allowProximitySort: proximitySortOrigin != nil) {
                    return preferred!
                } else {
                    return RouteSortMode.normal
                }
            }
        }()
        self.activeSortMode = activeSortMode
        let sortedByMode = StopIndexedRouteSearchResultEntryKt.bySortModes(casedResult, recentSortMode: recentSort, proximitySortOrigin: proximitySortOrigin)
        self.sortedByMode = sortedByMode
        self.sortedResults = sortedByMode[activeSortMode]!
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                LazyVStack {
                    if !ambientMode {
                        if recentSort == RecentSortMode.forced {
                            Button(action: {
                                registry().clearLastLookupRoutes(context: appContext())
                                appContext().popStack()
                            }) {
                                Image(systemName: "trash")
                                    .font(.system(size: 17.scaled()))
                                    .foregroundColor(.red)
                            }
                            .frame(width: 45.scaled(), height: 45.scaled())
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                            .edgesIgnoringSafeArea(.all)
                        } else if recentSort == RecentSortMode.choice || proximitySortOrigin != nil {
                            Button(action: {
                                activeSortMode = activeSortMode.nextMode(allowRecentSort: recentSort == RecentSortMode.choice, allowProximitySort: proximitySortOrigin != nil)
                            }) {
                                switch activeSortMode {
                                case RouteSortMode.proximity:
                                    Text(Shared().language == "en" ? "Sort: Proximity" : "排序: 巴士站距離").bold()
                                case RouteSortMode.recent:
                                    Text(Shared().language == "en" ? "Sort: Fav/Recent" : "排序: 喜歡/最近瀏覽").bold()
                                default:
                                    Text(Shared().language == "en" ? "Sort: Normal" : "排序: 正常").bold()
                                }
                            }
                            .font(.system(size: 17.scaled()))
                            .frame(width: 170.scaled(), height: 45.scaled())
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                            .edgesIgnoringSafeArea(.all)
                        }
                    }
                    ForEach(sortedResults, id: \.uniqueKey) { route in
                        RouteRow(route: route)
                        Divider()
                    }
                }
            }
        }
        .onReceive(timer) { _ in
            self.animationTick += 1
        }
        .onReceive(etaTimer) { _ in
            if showEta {
                for (uniqueKey, entry) in etaActive {
                    fetchEta(stopId: entry.stopInfo!.stopId, stopIndex: entry.stopInfoIndex, co: entry.co, route: entry.route!) { etaResults[uniqueKey] = $0 }
                }
            }
        }
        .onChange(of: activeSortMode) {
            let preferred = Shared().routeSortModePreference[listType]
            if preferred == nil || activeSortMode != preferred {
                registry().setRouteSortModePreference(context: appContext(), listType: listType, sortMode: activeSortMode)
            }
            withAnimation() { () -> () in
                sortedResults = sortedByMode[activeSortMode]!
            }
        }
        .onChange(of: isLuminanceReduced) {
            ambientMode = isLuminanceReduced && allowAmbient
        }
        .onAppear {
            ambientMode = isLuminanceReduced && allowAmbient
        }
    }
    
    func RouteRow(route: StopIndexedRouteSearchResultEntry) -> some View {
        let color = route.co.getColor(routeNumber: route.route!.routeNumber, elseColor: 0xFFFFFFFF as Int64).asColor()
        let kmbCtbJoint = route.route!.isKmbCtbJoint
        let dest = route.route!.resolvedDest(prependTo: true).get(language: Shared().language)
        let altSize = route.co == Operator.Companion().MTR && Shared().language != "en"
        let routeNumber = altSize ? Shared().getMtrLineName(lineName: route.route!.routeNumber) : route.route!.routeNumber
        let secondLine: [AttributedString] = {
            var list: [AttributedString] = []
            if route.stopInfo != nil {
                let stop = route.stopInfo!.data!
                list.append((stop.name.get(language: Shared().language)).asAttributedString())
            }
            if (kmbCtbJoint) {
                if Shared().language == "en" {
                    list.append((routeNumber.getKMBSubsidiary() == KMBSubsidiary.lwb ? "LWB" : "KMB").asAttributedString(color: color) + " CTB Joint Operated".asAttributedString(color: colorInt(0xFFFFE15E).asColor()))
                } else {
                    list.append((routeNumber.getKMBSubsidiary() == KMBSubsidiary.lwb ? "龍運" : "九巴").asAttributedString(color: color) + "城巴聯營線".asAttributedString(color: colorInt(0xFFFFE15E).asColor()))
                }
            }
            if route.co == Operator.Companion().NLB {
                list.append((Shared().language == "en" ? ("From " + route.route!.orig.en) : ("從" + route.route!.orig.zh + "開出")).asAttributedString(color: color.adjustBrightness(percentage: 0.75)))
            } else if route.co == Operator.Companion().KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.sunb {
                list.append((Shared().language == "en" ? ("Sun Bus (NR" + route.route!.orig.en + ")") : ("陽光巴士 (NR" + route.route!.orig.zh + ")")).asAttributedString(color: color.adjustBrightness(percentage: 0.75)))
            }
            return list
        }()
        
        return Button(action: {
            let meta: String
            switch route.co {
            case Operator.Companion().GMB:
                meta = route.route!.gmbRegion!.name
            case Operator.Companion().NLB:
                meta = route.route!.nlbId
            default:
                meta = ""
            }
            registry().addLastLookupRoute(routeNumber: route.route!.routeNumber, co: route.co, meta: meta, context: appContext())
            let data = newAppDataConatiner()
            data["route"] = route
            appContext().appendStack(screen: AppScreen.listStops, mutableData: data)
        }) {
            HStack(alignment: .center, spacing: 2.scaled()) {
                Text(routeNumber)
                    .frame(width: altSize ? 67.scaled() : 50.scaled(), alignment: .leading)
                    .font(.system(size: altSize ? 18.scaled() : 21.scaled()))
                    .foregroundColor(color)
                if secondLine.isEmpty {
                    MarqueeText(
                        text: dest,
                        font: UIFont.systemFont(ofSize: 17.scaled()),
                        leftFade: 8.scaled(),
                        rightFade: 8.scaled(),
                        startDelay: 2,
                        alignment: .bottomLeading
                    )
                    .foregroundColor(color)
                    .frame(maxWidth: .infinity, alignment: .leading)
                } else {
                    VStack(spacing: 0) {
                        MarqueeText(
                            text: dest,
                            font: UIFont.systemFont(ofSize: 17.scaled()),
                            leftFade: 8.scaled(),
                            rightFade: 8.scaled(),
                            startDelay: 2,
                            alignment: .bottomLeading
                        )
                        .foregroundColor(color)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        CrossfadeMarqueeText(
                            textList: secondLine,
                            state: isLuminanceReduced ? 0 : animationTick,
                            font: UIFont.systemFont(ofSize: altSize ? 11.scaled() : 12.scaled()),
                            leftFade: 8.scaled(),
                            rightFade: 8.scaled(),
                            startDelay: 2,
                            alignment: .bottomLeading
                        )
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: 0.75))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                if showEta {
                    let optEta = etaResults[route.uniqueKey]
                    if optEta != nil && optEta! != nil {
                        let eta = optEta!!
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
                                let text2 = "\n" + shortText.second
                                let text = text1.asAttributedString(fontSize: 17.scaled()) + text2.asAttributedString(fontSize: 8.scaled())
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
            }.contentShape(Rectangle())
        }
        .frame(width: 170.scaled(), height: 35.scaled())
        .buttonStyle(PlainButtonStyle())
        .transition(AnyTransition.scale)
        .onAppear {
            if showEta {
                etaActive[route.uniqueKey] = route
                fetchEta(stopId: route.stopInfo!.stopId, stopIndex: route.stopInfoIndex, co: route.co, route: route.route!) { etaResults[route.uniqueKey] = $0 }
            }
        }
        .onDisappear {
            etaActive.removeValue(forKey: route.uniqueKey)
        }
    }
}

#Preview {
    ListRoutesView(data: [:], storage: KotlinMutableDictionary())
}
