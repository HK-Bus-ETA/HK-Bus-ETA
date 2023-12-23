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

struct EtaView: View {
    
    @State private var eta: Registry.ETAQueryResult? = nil
    let etaTimer = Timer.publish(every: Double(Shared().ETA_UPDATE_INTERVAL) / 1000, on: .main, in: .common).autoconnect()
    
    @State private var stopId: String
    @State private var co: Operator
    @State private var index: Int
    @State private var stop: Stop
    @State private var route: Route
    @State private var offsetStart: Int
    
    @State private var stopList: [Registry.StopData]
    
    init(data: [String: Any]?) {
        self.stopId = data?["stopId"] as! String
        let co = data?["co"] as! Operator
        self.co = co
        self.index = data?["index"] as! Int
        self.stop = data?["stop"] as! Stop
        let route = data?["route"] as! Route
        self.route = route
        self.offsetStart = data?["offsetStart"] as? Int ?? 0
        
        self.stopList = registry().getAllStops(routeNumber: route.routeNumber, bound: co == Operator.Companion().NLB ? route.nlbId : route.bound[co]!, co: co, gmbRegion: route.gmbRegion)
    }
    
    var body: some View {
        ZStack {
            VStack(alignment: .center, spacing: 3) {
                Button(action: {
                    print("Button tapped!")
                }) {
                    VStack(alignment: .center) {
                        Text(co.isTrain ? stop.name.get(language: Shared().language) : "\(index). \(stop.name.get(language: Shared().language))")
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .autoResizing(maxSize: 23)
                            .lineLimit(2)
                        let destName = registry().getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true)
                        Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false) + " " + destName.get(language: Shared().language))
                            .foregroundColor(0xFFFFFFFF.asColor())
                            .autoResizing(maxSize: 12)
                            .lineLimit(1)
                    }
                }
                .buttonStyle(PlainButtonStyle())
                Spacer(minLength: 10)
                ETALine(line: eta, seq: 1)
                ETALine(line: eta, seq: 2)
                ETALine(line: eta, seq: 3)
                Spacer(minLength: 10)
                HStack(alignment: /*@START_MENU_TOKEN@*/.center/*@END_MENU_TOKEN@*/, spacing: 3) {
                    Button(action: {
                        let data = newAppDataConatiner()
                        let newStopData = stopList[index - 2]
                        data["stopId"] = newStopData.stopId
                        data["co"] = co
                        data["index"] = index - 1
                        data["stop"] = newStopData.stop
                        data["route"] = newStopData.route
                        appContext().appendStack(screen: AppScreen.dummy)
                        appContext().popSecondLastStack()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            appContext().appendStack(screen: AppScreen.eta, mutableData: data)
                            appContext().popSecondLastStack()
                        }
                    }) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 17))
                            .foregroundColor(index > 1 ? 0xFFFFFFFF.asColor() : 0xFF494949.asColor())
                    }
                    .frame(width: 25, height: 25)
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .edgesIgnoringSafeArea(.all)
                    .disabled(index <= 1)
                    
                    Button(action: {
                        print("Button tapped!")
                    }) {
                        Text(Shared().language == "en" ? "More" : "更多")
                    }
                    .frame(width: 55, height: 25)
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .edgesIgnoringSafeArea(.all)
                    
                    Button(action: {
                        let data = newAppDataConatiner()
                        let newStopData = stopList[index]
                        data["stopId"] = newStopData.stopId
                        data["co"] = co
                        data["index"] = index + 1
                        data["stop"] = newStopData.stop
                        data["route"] = newStopData.route
                        appContext().appendStack(screen: AppScreen.dummy)
                        appContext().popSecondLastStack()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            appContext().appendStack(screen: AppScreen.eta, mutableData: data)
                            appContext().popSecondLastStack()
                        }
                    }) {
                        Image(systemName: "arrow.right")
                            .font(.system(size: 17))
                            .foregroundColor(index < stopList.count ? 0xFFFFFFFF.asColor() : 0xFF494949.asColor())
                    }
                    .frame(width: 25, height: 25)
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .edgesIgnoringSafeArea(.all)
                    .disabled(index >= stopList.count)
                }
            }
            BackButton { $0.screen == AppScreen.eta }
        }
        .onReceive(etaTimer) { _ in
            Task { await fetchEta() }
        }
        .onAppear {
            Task { await fetchEta() }
        }
    }
    
    func ETALine(line: Registry.ETAQueryResult?, seq: Int) -> some View {
        let text = line?.getLine(index: seq.asInt32()).text?.asAttributedString(defaultFontSize: 22) ?? (seq == 1 ? (Shared().language == "en" ? "Updating" : "更新中") : "").asAttributedString()
        return MarqueeText(
            text: text,
            font: UIFont.systemFont(ofSize: 22),
            leftFade: 8,
            rightFade: 8,
            startDelay: 2,
            alignment: .center
        )
        .foregroundColor(0xFFFFFFFF.asColor().adjustBrightness(percentage: line == nil ? 0.7 : 1))
        .lineLimit(1)
    }
    
    func fetchEta() async {
        eta = registry().getEta(stopId: stopId, stopIndex: index.asInt32(), co: co, route: route, context: appContext()).get(timeout: Shared().ETA_UPDATE_INTERVAL, unit: Kotlinx_datetimeDateTimeUnit.Companion().MILLISECOND)
    }
}

#Preview {
    EtaView(data: nil)
}
