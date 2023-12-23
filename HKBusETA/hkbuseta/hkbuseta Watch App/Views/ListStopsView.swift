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
    
    @State private var animationTick = 0
    
    let timer = Timer.publish(every: 5.5, on: .main, in: .common).autoconnect()
    
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
    
    init(data: [String: Any]?) {
        let route = data?["route"] as! RouteSearchResultEntry
        self.route = route
        self.scrollToStop = data?["scrollToStop"] as? String
        self.showEta = data?["showEta"] as? Bool ?? true
        self.isAlightReminder = data?["isAlightReminder"] as? Bool ?? false
        
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
        self.specialOrigs = specialOrigsDests.first!.map { $0 as! BilingualText }.filter { $0.zh.eitherContains(other: origName.zh) }
        self.specialDests = specialOrigsDests.second!.map { $0 as! BilingualText }.filter { $0.zh.eitherContains(other: destName.zh) }
        self.coColor = co.getColor(routeNumber: routeNumber, elseColor: 0xFFFFFFFF).asColor()
        let stopList = registry().getAllStops(routeNumber: routeNumber, bound: bound, co: co, gmbRegion: gmbRegion)
        self.stopList = stopList
        self.lowestServiceType = stopList.min { $0.serviceType < $1.serviceType }!.serviceType
    }
    
    var body: some View {
        ZStack {
            ScrollViewReader { value in
                ScrollView(.vertical) {
                    LazyVStack {
                        VStack(alignment: /*@START_MENU_TOKEN@*/.center/*@END_MENU_TOKEN@*/, spacing: 2) {
                            Text(co.getDisplayName(routeNumber: routeNumber, kmbCtbJoint: kmbCtbJoint, language: Shared().language, elseName: "???") + " " + co.getDisplayRouteNumber(routeNumber: routeNumber, shortened: false))
                                .foregroundColor(coColor)
                                .autoResizing(maxSize: 23)
                                .bold()
                                .lineLimit(1)
                            Text(resolvedDestName.get(language: Shared().language))
                                .foregroundColor(0xFFFFFFFF.asColor())
                                .autoResizing(maxSize: 12)
                                .lineLimit(2)
                            if !specialOrigs.isEmpty {
                                Text(Shared().language == "en" ? ("Special From " + specialOrigs.map { $0.en }.joined(separator: "/")) : ("特別班 從" + specialOrigs.map { $0.zh }.joined(separator: "/") + "開出"))
                                    .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.65))
                                    .lineLimit(2)
                                    .autoResizing(maxSize: 12)
                            }
                            if !specialDests.isEmpty {
                                Text(Shared().language == "en" ? ("Special To " + specialDests.map { $0.en }.joined(separator: "/")) : ("特別班 往" + specialDests.map { $0.zh }.joined(separator: "/")))
                                    .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: 0.65))
                                    .lineLimit(2)
                                    .autoResizing(maxSize: 12)
                            }
                        }
                        ForEach(stopList.indices, id: \.self) { index in
                            StopRow(index: index)
                            Divider()
                        }
                    }
                }
            }
            BackButton { $0.screen == AppScreen.listStops }
        }
        .onReceive(timer) { _ in
            self.animationTick += 1
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
            HStack(alignment: .center, spacing: 2) {
                Text("\(stopNumber).")
                    .frame(width: 37, alignment: .leading)
                    .font(.system(size: 18))
                    .foregroundColor(0xFFFFFFFF.asColor())
                MarqueeText(
                    text: stopData.stop.remarkedName.get(language: Shared().language).asAttributedString(defaultFontSize: 18),
                    font: UIFont.systemFont(ofSize: 18),
                    leftFade: 8,
                    rightFade: 8,
                    startDelay: 2,
                    alignment: .bottomLeading
                )
                .foregroundColor(0xFFFFFFFF.asColor())
                .frame(maxWidth: .infinity, alignment: .leading)
            }.contentShape(Rectangle())
        }
        .frame(width: 170, height: 30)
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    ListStopsView(data: nil)
}
