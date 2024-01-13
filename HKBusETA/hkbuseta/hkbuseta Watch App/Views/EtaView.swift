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
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    @State private var stopId: String
    @State private var co: Operator
    @State private var index: Int
    @State private var stop: Stop
    @State private var route: Route
    @State private var offsetStart: Int
    
    @State private var stopList: [Registry.StopData]
    @State private var clockTimeMode: Bool
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.stopId = data["stopId"] as! String
        let co = data["co"] as! Operator
        self.co = co
        self.index = data["index"] as! Int
        self.stop = data["stop"] as! Stop
        let route = data["route"] as! Route
        self.route = route
        self.offsetStart = data["offsetStart"] as? Int ?? 0
        
        self.stopList = registry(appContext).getAllStops(routeNumber: route.routeNumber, bound: co == Operator.Companion().NLB ? route.nlbId : route.bound[co]!, co: co, gmbRegion: route.gmbRegion)
        self.clockTimeMode = Shared().clockTimeMode
    }
    
    var body: some View {
        VStack(alignment: .center, spacing: 3.scaled(appContext)) {
            Text(co.isTrain ? stop.name.get(language: Shared().language) : "\(index). \(stop.name.get(language: Shared().language))")
                .multilineTextAlignment(.center)
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(2)
                .autoResizing(maxSize: 23.scaled(appContext), weight: .bold)
            let destName = registry(appContext).getStopSpecialDestinations(stopId: stopId, co: co, route: route, prependTo: true)
            Text(co.getDisplayRouteNumber(routeNumber: route.routeNumber, shortened: false) + " " + destName.get(language: Shared().language))
                .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: ambientMode ? 0.7 : 1))
                .lineLimit(1)
                .autoResizing(maxSize: 12.scaled(appContext))
            Spacer(minLength: 7.scaled(appContext))
            ETALine(lines: eta, seq: 1)
            ETALine(lines: eta, seq: 2)
            ETALine(lines: eta, seq: 3)
            Spacer(minLength: 7.scaled(appContext))
            if ambientMode {
                Spacer(minLength: 25.scaled(appContext))
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
                            .font(.system(size: 17.scaled(appContext)))
                            .foregroundColor(index > 1 ? colorInt(0xFFFFFFFF).asColor() : colorInt(0xFF494949).asColor())
                    }
                    .frame(width: 25.scaled(appContext), height: 25.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .edgesIgnoringSafeArea(.all)
                    .disabled(index <= 1)
                    
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
                            .font(.system(size: 17.scaled(appContext)))
                    }
                    .frame(width: 65.scaled(appContext), height: 25.scaled(appContext))
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
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.dummy))
                        appContext.finish()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.eta, data))
                        }
                    }) {
                        Image(systemName: "arrow.right")
                            .font(.system(size: 17.scaled(appContext)))
                            .foregroundColor(index < stopList.count ? colorInt(0xFFFFFFFF).asColor() : colorInt(0xFF494949).asColor())
                    }
                    .frame(width: 25.scaled(appContext), height: 25.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 25))
                    .edgesIgnoringSafeArea(.all)
                    .disabled(index >= stopList.count)
                }
            }
        }
        .frame(height: CGFloat(appContext.screenHeight) * 0.8)
        .onReceive(etaTimer) { _ in
            fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: route) { eta = $0 }
        }
        .onAppear {
            fetchEta(appContext: appContext, stopId: stopId, stopIndex: index, co: co, route: route) { eta = $0 }
        }
        .gesture(
            TapGesture()
                .onEnded { _ in
                    clockTimeMode = !clockTimeMode
                    registry(appContext).setClockTimeMode(clockTimeMode: clockTimeMode, context: appContext)
                }
        )
    }
    
    func ETALine(lines: Registry.ETAQueryResult?, seq: Int) -> some View {
        let text = Shared().getResolvedText(lines, seq: seq.asInt32(), clockTimeMode: clockTimeMode, context: appContext).asAttributedString(defaultFontSize: 20.scaled(appContext))
        return MarqueeText(
            text: text,
            font: UIFont.systemFont(ofSize: 20.scaled(appContext)),
            leftFade: 8.scaled(appContext),
            rightFade: 8.scaled(appContext),
            startDelay: 2,
            alignment: .center
        )
        .foregroundColor(colorInt(0xFFFFFFFF).asColor().adjustBrightness(percentage: lines == nil || (ambientMode && seq > 1) ? 0.7 : 1))
        .lineLimit(1)
    }

}
