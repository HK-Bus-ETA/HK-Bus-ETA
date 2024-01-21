//
//  hkbusetaApp.swift
//  hkbuseta
//
//  Created by LOOHP on 20/01/2024.
//

import SwiftUI
import WatchConnectivity

class ApplicationDelegate: NSObject, UIApplicationDelegate, WCSessionDelegate {
    
    override init() {
        super.init()
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }
    
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        
    }
    
    func sessionDidBecomeInactive(_ session: WCSession) {
        
    }
    
    func sessionDidDeactivate(_ session: WCSession) {
        
    }
    
}

let HKBUSAPP_URL_PATTERN = regex("https://(?:(?:watch|wear)\\.)?hkbus\\.app/(?:.+/)?route/([^/]*)(?:/([^/]*)(?:%2C|,)([^/]*))?")

@main
struct hkbusetaApp: App {
    
    let refreshTimer = Timer.publish(every: 2, on: .main, in: .common).autoconnect()
    
    @UIApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate
    
    @State var lastSharePayload: [String: Any]? = nil
    @State var watchAppInstalled = WCSession.default.isWatchAppInstalled
    
    var body: some Scene {
        WindowGroup {
            VStack(alignment: .leading, spacing: 40) {
                VStack(alignment: .center) {
                    Image("icon_full")
                        .imageScale(.large)
                        .foregroundStyle(.tint)
                }
                .frame(maxWidth: .infinity)
                .highPriorityGesture(
                    TapGesture()
                        .onEnded { _ in
                            if let url = URL(string: "https://apps.apple.com/app/id6475241017") {
                                UIApplication.shared.open(url)
                            }
                        }
                )
                VStack(alignment: .center) {
                    Text("香港巴士到站預報 (WatchOS)")
                        .multilineTextAlignment(.center)
                        .font(.title.bold())
                    Text("HK Bus ETA (WatchOS)")
                        .multilineTextAlignment(.center)
                        .font(.title.bold())
                    Text("@LoohpJames")
                        .multilineTextAlignment(.center)
                        .font(.subheadline)
                }
                .highPriorityGesture(
                    TapGesture()
                        .onEnded { _ in
                            if let url = URL(string: "https://loohpjames.com") {
                                UIApplication.shared.open(url)
                            }
                        }
                )
                .frame(maxWidth: .infinity)
                VStack(alignment: .leading) {
                    Text("本手機配套應用程式用於直接讓你在手錶上開啟巴士路線連結。\n使用此功能時，手錶應用程式必須位於前台。")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                    Text("This phone companion app is used for directly launching into bus routes and stops onto the watch app.\nThe watch app must be in the foreground while this feature is used.")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                }
                .frame(maxWidth: .infinity)
                if watchAppInstalled {
                    Button(action: {
                        if lastSharePayload != nil {
                            WCSession.default.sendMessage(lastSharePayload!) { _ in }
                        }
                    }) {
                        Text("重新開啟上一條路線 Relaunch Last Route")
                    }
                    .frame(maxWidth: .infinity)
                    .disabled(lastSharePayload == nil)
                } else {
                    Button(action: {
                        UIApplication.shared.open(URL(string: "https://apps.apple.com/us/app/hk-bus-eta-watchos/id6475241017")!)
                    }) {
                        Text("下載手錶應用程式 Download Watch App")
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .onReceive(refreshTimer) { _ in
                watchAppInstalled = WCSession.default.isWatchAppInstalled
            }
            .onOpenURL { url in
                do {
                    if let matcher = try HKBUSAPP_URL_PATTERN.firstMatch(in: url.absoluteString) {
                        let key = String(matcher[1].value as! Substring)

                        var payload: Dictionary<String, Any> = [:]
                        payload["k"] = key
                        
                        if matcher.count >= 3 {
                            let group2 = String(matcher[2].value as! Substring)
                            if !group2.isEmpty {
                                payload["s"] = group2
                            }
                        }
                        if matcher.count >= 4 {
                            let group3 = Int(String(matcher[3].value as! Substring))
                            payload["si"] = group3
                        }
                        
                        lastSharePayload = payload
                        WCSession.default.sendMessage(lastSharePayload!) { _ in }
                    }
                } catch {
                    
                }
            }
        }
    }
}

func regex(_ pattern: String) -> Regex<AnyRegexOutput> {
    do {
        return try Regex(pattern)
    } catch {
        fatalError()
    }
}
