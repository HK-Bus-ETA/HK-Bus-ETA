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

struct ListStopsView: View {
    
    @ObservedObject private var locationManager = SingleLocationManager()
    @State private var scrollTarget: Int? = nil
    @State private var scrolled = false
    
    @State private var animationTick = 0
    
    let timer = Timer.publish(every: 5.5, on: .main, in: .common).autoconnect()
    
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    @State private var etaActive: [Int] = []
    @State private var etaResults: [Int: Registry.ETAQueryResult?] = [:]
    
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
    @State private var coColor: Color
    @State private var stopList: [Registry.StopData]
    @State private var lowestServiceType: Int32
    @State private var mtrStopsInterchange: [Registry.MTRInterchangeData]
    @State private var mtrLineSectionData: [MTRStopSectionData]
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
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
        let specialOrigsDests = registry().getAllOriginsAndDestinations(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.specialOrigs = specialOrigsDests.first!.map { $0 as! BilingualText }.filter { !$0.zh.eitherContains(other: origName.zh) }
        self.specialDests = specialOrigsDests.second!.map { $0 as! BilingualText }.filter { !$0.zh.eitherContains(other: destName.zh) }
        self.coColor = co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF).asColor()
        let stopList = registry().getAllStops(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.stopList = stopList
        self.lowestServiceType = stopList.min { $0.serviceType < $1.serviceType }!.serviceType
        if co.isTrain {
            let mtrStopsInterchange = stopList.map { registry().getMtrStationInterchange(stopId: $0.stopId, lineName: routeNumber) }
            self.mtrStopsInterchange = mtrStopsInterchange
            self.mtrLineSectionData = MTRRouteMapUtilsKt.createMTRLineSectionData(co: co, color: co.getLineColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF), stopList: stopList, mtrStopsInterchange: mtrStopsInterchange, isLrtCircular: route.route!.lrtCircular != nil, context: appContext())
        } else {
            self.mtrStopsInterchange = []
            self.mtrLineSectionData = []
        }
    }
    
    var body: some View {
        ScrollViewReader { value in
            ScrollView(.vertical) {
                LazyVStack(spacing: 0) {
                    VStack(alignment: /*@START_MENU_TOKEN@*/.center/*@END_MENU_TOKEN@*/, spacing: 2.scaled()) {
                        Text(co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: kmbCtbJoint, language: Shared().language, elseName: "???") + " " + co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))
                            .foregroundColor(coColor)
                            .lineLimit(1)
                            .autoResizing(maxSize: 23.scaled())
                            .bold()
                        Text(resolvedDestName.get(language: Shared().language))
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .lineLimit(2)
                            .autoResizing(maxSize: 12.scaled())
                        if !specialOrigs.isEmpty {
                            Text(Shared().language == "en" ? ("Special From " + specialOrigs.map { $0.en }.joined(separator: "/")) : ("特別班 從" + specialOrigs.map { $0.zh }.joined(separator: "/") + "開出"))
                                .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.65))
                                .lineLimit(2)
                                .autoResizing(maxSize: 12.scaled())
                        }
                        if !specialDests.isEmpty {
                            Text(Shared().language == "en" ? ("Special To " + specialDests.map { $0.en }.joined(separator: "/")) : ("特別班 往" + specialDests.map { $0.zh }.joined(separator: "/")))
                                .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.65))
                                .lineLimit(2)
                                .autoResizing(maxSize: 12.scaled())
                        }
                    }
                    ForEach(stopList.indices, id: \.self) { index in
                        StopRow(index: index).id(index)
                        Divider()
                    }
                }
            }
            .onChange(of: scrollTarget) {
                if !scrolled && scrollTarget != nil {
                    value.scrollTo(scrollTarget!, anchor: .center)
                    scrolled = true
                }
            }
        }
        .onReceive(timer) { _ in
            self.animationTick += 1
        }
        .onReceive(etaTimer) { _ in
            if showEta {
                for index in etaActive {
                    fetchEta(stopId: stopList[index].stopId, stopIndex: index, co: co, route: route.route!) { etaResults[index] = $0 }
                }
            }
        }
        .onChange(of: locationManager.readyForRequest) {
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
        .onChange(of: locationManager.isLocationFetched) {
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
                    scrollTarget = Int(closest!.stopIndex) - 1
                }
            }
        }
    }
    
    func StopRow(index: Int) -> some View {
        let stopData = stopList[index]
        let stopNumber = index + 1
        return Button(action: {
            let data = newAppDataConatiner()
            data["stopId"] = stopData.stopId
            data["co"] = co
            data["index"] = stopNumber
            data["stop"] = stopData.stop
            data["route"] = stopData.route
            appContext().appendStack(screen: AppScreen.eta, mutableData: data)
        }) {
            HStack(alignment: .center, spacing: 2.scaled()) {
                if co.isTrain && !mtrLineSectionData.isEmpty {
                    let width: CGFloat = Set(stopList.map { $0.serviceType }).count > 1 || mtrStopsInterchange.contains(where: { !$0.outOfStationLines.isEmpty }) ? 64 : 47
                    MTRLineSection(sectionData: mtrLineSectionData[index])
                        .frame(minWidth: width, maxWidth: width, maxHeight: .infinity)
                } else {
                    Text("\(stopNumber).")
                        .frame(width: 37.scaled(), alignment: .leading)
                        .font(.system(size: 18.scaled()))
                        .foregroundColor(0xFFFFFFFF.asColor())
                }
                MarqueeText(
                    text: stopData.stop.remarkedName.get(language: Shared().language).asAttributedString(defaultFontSize: 18.scaled()),
                    font: UIFont.systemFont(ofSize: 18.scaled()),
                    leftFade: 8.scaled(),
                    rightFade: 8.scaled(),
                    startDelay: 2,
                    alignment: .bottomLeading
                )
                .foregroundColor(0xFFFFFFFF.asColor())
                .frame(maxWidth: .infinity, alignment: .leading)
                if showEta {
                    let optEta = etaResults[index]
                    if optEta != nil && optEta! != nil {
                        let eta = optEta!!
                        if !eta.isConnectionError {
                            if !(0..<60).contains(eta.nextScheduledBus) {
                                if eta.isMtrEndOfLine {
                                    Image(systemName: "arrow.forward.to.line.circle")
                                        .font(.system(size: 17.scaled()))
                                        .foregroundColor(0xFF798996.asColor())
                                } else if (eta.isTyphoonSchedule) {
                                    Image(systemName: "hurricane")
                                        .font(.system(size: 17.scaled()))
                                        .foregroundColor(0xFF798996.asColor())
                                } else {
                                    Image(systemName: "clock")
                                        .font(.system(size: 17.scaled()))
                                        .foregroundColor(0xFF798996.asColor())
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
                                    .foregroundColor(0xFF798996.asColor())
                                    .lineLimit(2)
                            }
                        }
                    }
                }
            }.contentShape(Rectangle())
        }
        .frame(width: 170.scaled(), height: 40.scaled())
        .buttonStyle(PlainButtonStyle())
        .onAppear {
            if showEta {
                etaActive.append(index)
                fetchEta(stopId: stopData.stopId, stopIndex: index, co: co, route: route.route!) { etaResults[index] = $0 }
            }
        }
        .onDisappear {
            etaActive.removeAll(where: { $0 == index })
        }
    }
}

#Preview {
    ListStopsView(data: [:], storage: KotlinMutableDictionary())
}
