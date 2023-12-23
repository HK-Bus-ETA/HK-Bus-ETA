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
    
    @State var result: [StopIndexedRouteSearchResultEntry]
    @State var listType: RouteListType
    @State var showEta: Bool
    @State var recentSort: RecentSortMode
    @State var proximitySortOrigin: Coordinates?
    @State var allowAmbient: Bool
    
    init(data: [String: Any]?) {
        let rawResult = data!["result"]! as! [Any]
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
                    $0.stopId == stopInfo?.stopId
                })!.asInt32()
            }
        }
        self.result = casedResult
        self.listType = data?["listType"] as? RouteListType ?? RouteListType.Companion().NORMAL
        self.showEta = data?["showEta"] as? Bool ?? false
        self.recentSort = data?["recentSort"] as? RecentSortMode ?? RecentSortMode.disabled
        self.proximitySortOrigin = data?["proximitySortOrigin"] as? Coordinates
        self.allowAmbient = data?["allowAmbient"] as? Bool ?? false
    }
    
    var body: some View {
        ZStack {
            ScrollView(.vertical) {
                LazyVStack {
                    ForEach(result, id: \.uniqueKey) { route in
                        let color = route.co.getColor(routeNumber: route.route!.routeNumber, elseColor: 0xFFFFFFFF).asColor()
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
                                    list.append("九巴".asAttributedString(color: color) + "城巴聯營線".asAttributedString(color: 0xFFFFE15E.asColor()))
                                } else {
                                    list.append("KMB ".asAttributedString(color: color) + "CTB Joint Operated".asAttributedString(color: 0xFFFFE15E.asColor()))
                                }
                            }
                            if route.co == Operator.Companion().NLB {
                                list.append((Shared().language == "en" ? ("From " + route.route!.orig.en) : ("從" + route.route!.orig.zh + "開出")).asAttributedString(color: color.adjustBrightness(percentage: 0.75)))
                            } else if route.co == Operator.Companion().KMB && RouteExtensionsKt.getKMBSubsidiary(routeNumber) == KMBSubsidiary.sunb {
                                list.append((Shared().language == "en" ? ("Sun Bus (NR" + route.route!.orig.en + ")") : ("陽光巴士 (NR" + route.route!.orig.zh + ")")).asAttributedString(color: color.adjustBrightness(percentage: 0.75)))
                            }
                            return list
                        }()
                        
                        Button(action: {
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
                            HStack(alignment: .center, spacing: 2) {
                                Text(routeNumber)
                                    .frame(width: altSize ? 67 : 47, alignment: .leading)
                                    .font(.system(size: altSize ? 18 : 21))
                                    .foregroundColor(color)
                                if secondLine.isEmpty {
                                    MarqueeText(
                                        text: dest,
                                        font: UIFont.systemFont(ofSize: 17),
                                        leftFade: 8,
                                        rightFade: 8,
                                        startDelay: 2,
                                        alignment: .bottomLeading
                                    )
                                    .foregroundColor(color)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                } else {
                                    VStack(spacing: 0) {
                                        MarqueeText(
                                            text: dest,
                                            font: UIFont.systemFont(ofSize: 17),
                                            leftFade: 8,
                                            rightFade: 8,
                                            startDelay: 2,
                                            alignment: .bottomLeading
                                        )
                                        .foregroundColor(color)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        CrossfadeMarqueeText(
                                            textList: secondLine,
                                            state: animationTick,
                                            font: UIFont.systemFont(ofSize: altSize ? 11 : 12),
                                            leftFade: 8,
                                            rightFade: 8,
                                            startDelay: 2,
                                            alignment: .bottomLeading
                                        )
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                }
                            }.contentShape(Rectangle())
                        }
                        .frame(width: 170, height: 35)
                        .buttonStyle(PlainButtonStyle())
                        Divider()
                    }
                }
            }
            BackButton { $0.screen == AppScreen.listRoutes }
        }
        .onReceive(timer) { _ in
            self.animationTick += 1
        }
    }
}

#Preview {
    ListRoutesView(data: nil)
}
