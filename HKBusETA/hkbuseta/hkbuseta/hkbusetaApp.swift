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

let SHARE_ULR_PREFIX = "https://watch.hkbus.app/shared?url="
let KMB_URL_STARTS_WITH = "https://app1933.page.link"
let KMB_DIRECT_URL_STARTS_WITH = "https://m4.kmb.hk/kmb-ws/share.php?parameter="
let CTB_URL_PATTERN = regex("(?:城巴|Citybus) ?App: ?([0-9A-Za-z]+) ?(?:往|To) ?(.*)?http")
let HKBUSAPP_URL_PATTERN = regex("https://(?:(?:watch|wear)\\.)?hkbus\\.app/(?:.+/)?route/([^/]*)(?:/([^/]*)(?:%2C|,)([^/]*))?")

@main
struct hkbusetaApp: App {
    
    let refreshTimer = Timer.publish(every: 2, on: .main, in: .common).autoconnect()
    
    @UIApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate
    
    @State var lastSharePayload: [String: Any]? = nil
    @State var invalidLink = false
    @State var watchAppInstalled = WCSession.default.isWatchAppInstalled
    
    var body: some Scene {
        WindowGroup {
            VStack(alignment: .leading, spacing: 40) {
                VStack(alignment: .center) {
                    Image("icon_full")
                        .resizable()
                        .frame(width: 150, height: 150)
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
                    Text("本手機配套應用程式用於直接讓你在手錶上開啟巴士路線連結。")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                    Text("使用此功能時，手錶應用程式必須位於前台。")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                    Text("This phone companion app is used for directly launching into bus routes and stops onto the watch app.")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                    Text("The watch app must be in the foreground while this feature is used.")
                        .multilineTextAlignment(.leading)
                        .font(.title3)
                }
                .frame(maxWidth: .infinity)
                if invalidLink {
                    Text("不支援此巴士路線連結\nUnsupported Bus Route Link")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                } else if watchAppInstalled {
                    Button(action: {
                        if lastSharePayload != nil {
                            WCSession.default.sendMessage(lastSharePayload!) { _ in }
                        }
                    }) {
                        Text("重新開啟上一條路線 Relaunch Last Route")
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .disabled(lastSharePayload == nil)
                } else {
                    Button(action: {
                        UIApplication.shared.open(URL(string: "https://apps.apple.com/us/app/hk-bus-eta-watchos/id6475241017")!)
                    }) {
                        Text("下載手錶應用程式 Download Watch App")
                            .multilineTextAlignment(.center)
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
                    let shareUrl: String
                    if url.absoluteString.hasPrefix(SHARE_ULR_PREFIX) {
                        let urlStr = url.absoluteString
                        let index = urlStr.index(urlStr.startIndex, offsetBy: SHARE_ULR_PREFIX.count)
                        shareUrl = String(urlStr[index...])
                    } else {
                        shareUrl = url.absoluteString
                    }
                    
                    if shareUrl.hasPrefix(KMB_URL_STARTS_WITH) || shareUrl.hasPrefix(KMB_DIRECT_URL_STARTS_WITH) {
                        let realUrl = shareUrl.hasPrefix(KMB_URL_STARTS_WITH) ? getMovedRedirect(from: shareUrl) : shareUrl
                        guard let decodedUrl = realUrl?.removingPercentEncoding else { return }

                        let parameterPrefix = "https://m4.kmb.hk/kmb-ws/share.php?parameter="
                        guard decodedUrl.hasPrefix(parameterPrefix) else { return }
                        let parameter = String(decodedUrl.dropFirst(parameterPrefix.count))

                        guard let data = Data(base64Encoded: parameter) else { return }
                        guard let jsonStr = String(data: data, encoding: .utf8),
                              let jsonData = jsonStr.data(using: .utf8),
                              let jsonObject = try? JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] else { return }

                        let route = jsonObject["r"] as? String ?? ""
                        let companyCode = jsonObject["c"] as? String ?? ""
                        var co = "kmb"

                        if companyCode.hasPrefix("NL") {
                            co = "nlb"
                        } else if companyCode.hasPrefix("GB") {
                            co = "gmb"
                        }

                        var payload: [String: Any] = ["r": route, "c": co]
                        if co == "kmb", let b = jsonObject["b"] as? String {
                            payload["b"] = b == "1" ? "O" : "I"
                        }
                        
                        lastSharePayload = payload
                        WCSession.default.sendMessage(payload) { _ in }
                        invalidLink = false
                        return
                    } else if let matcher = try CTB_URL_PATTERN.firstMatch(in: shareUrl) {
                        let route = String(matcher[1].value as! Substring)
                        let dest = String(matcher[2].value as! Substring)
                        
                        var payload: Dictionary<String, Any> = [:]
                        payload["r"] = route
                        payload["d"] = dest.trimmingCharacters(in: CharacterSet(charactersIn: " "))

                        lastSharePayload = payload
                        WCSession.default.sendMessage(payload) { _ in }
                        invalidLink = false
                        return
                    } else if let matcher = try HKBUSAPP_URL_PATTERN.firstMatch(in: shareUrl) {
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
                        WCSession.default.sendMessage(payload) { _ in }
                        invalidLink = false
                        return
                    }
                } catch {
                    
                }
                invalidLink = true
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

func getMovedRedirect(from link: String) -> String? {
    guard let url = URL(string: link) else {
        return nil
    }

    var request = URLRequest(url: url)
    request.httpMethod = "GET"
    request.setValue("Mozilla/5.0", forHTTPHeaderField: "User-Agent")
    request.setValue("no-cache, no-store, must-revalidate", forHTTPHeaderField: "Cache-Control")
    request.setValue("no-cache", forHTTPHeaderField: "Pragma")
    request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData

    let semaphore = DispatchSemaphore(value: 0)
    var location: String?

    let task = URLSession.shared.dataTask(with: request) { _, response, error in
        defer { semaphore.signal() }
        
        guard error == nil else {
            return
        }

        if let httpResponse = response as? HTTPURLResponse,
           (httpResponse.statusCode == 301 || httpResponse.statusCode == 302),
           let newLocation = httpResponse.allHeaderFields["Location"] as? String {
            location = newLocation
        }
    }
    task.resume()

    semaphore.wait()
    return location
}
