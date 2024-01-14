//
//  Shared.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import Foundation
import WatchKit
import SwiftUI
import shared
import AuthenticationServices

let watchOSVersion = WKInterfaceDevice.current().systemVersion
let belowWidgetVersion = watchOSVersion.compare("9.0", options: .numeric) == .orderedAscending

func applicationContext() -> AppContextWatchOS {
    return AppContextWatchOSKt.applicationContext
}

func registry(_ appContext: AppContext) -> Registry {
    return Registry.Companion().getInstance(context: appContext)
}

func registryNoUpdate(_ appContext: AppContext) -> Registry {
    return Registry.Companion().getInstanceNoUpdateCheck(context: appContext)
}

func fetchEta(appContext: AppContext, stopId: String, stopIndex: Int, co: Operator, route: Route, callback: @escaping (Registry.ETAQueryResult) -> Void) {
    fetchEta(appContext: appContext, stopId: stopId, stopIndex: stopIndex.asInt32(), co: co, route: route, callback: callback)
}

func fetchEta(appContext: AppContext, stopId: String, stopIndex: Int32, co: Operator, route: Route, callback: @escaping (Registry.ETAQueryResult) -> Void) {
    let pending = registry(appContext).getEta(stopId: stopId, stopIndex: stopIndex, co: co, route: route, context: appContext)
    pending.onComplete(timeout: Shared().ETA_UPDATE_INTERVAL, unit: Kotlinx_datetimeDateTimeUnit.Companion().MILLISECOND, callback: { result in
        DispatchQueue.main.async {
            callback(result)
        }
    })
}

func playHaptics() {
    AppContextWatchOSKt.hapticFeedback.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
}

func newAppDataConatiner() -> KotlinMutableDictionary<NSString, AnyObject> {
    return AppContextWatchOSKt.createMutableAppDataContainer()
}

func newAppIntent(_ context: AppContext, _ screen: AppScreen, _ data: KotlinMutableDictionary<NSString, AnyObject> = KotlinMutableDictionary()) -> AppIntent {
    return AppContextWatchOSKt.createAppIntent(context: context, screen: screen, appDataContainer: data)
}

func dispatcherIO(task: @escaping () -> Void) {
    AppContextWatchOSKt.dispatcherIO(task: task)
}

func openUrl(link: String) {
    guard let url = URL(string: link) else {
        return
    }
    let session = ASWebAuthenticationSession(url: url,callbackURLScheme: nil) { _, _ in }
    session.prefersEphemeralWebBrowserSession = true
    session.start()
}

func openMaps(lat: Double, lng: Double, label: String) {
    let coordinate = CLLocationCoordinate2DMake(lat, lng)
    let placemark = MKPlacemark(coordinate: coordinate)
    let mapItem = MKMapItem(placemark: placemark)
    mapItem.name = label
    mapItem.timeZone = TimeZone(identifier: "Asia/Hong_Kong")
    mapItem.openInMaps()
}

extension View {
    
    func CrossfadeText(textList: [AttributedString], state: Int) -> some View {
        Text(textList[state % textList.count])
            .id(textList[state % textList.count])
            .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    func CrossfadeMarqueeText(textList: [AttributedString], state: Int, font: UIFont, leftFade: CGFloat, rightFade: CGFloat, startDelay: Double, alignment: Alignment? = nil) -> some View {
        MarqueeText(
            text: textList[state % textList.count],
            font: font,
            leftFade: leftFade,
            rightFade: rightFade,
            startDelay: startDelay,
            alignment: alignment
        )
        .id(textList[state % textList.count])
        .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    func autoResizing(maxSize: CGFloat = 200, minSize: CGFloat = 1, weight: Font.Weight = .regular) -> some View {
        self.font(.system(size: maxSize, weight: weight)).minimumScaleFactor(minSize / maxSize)
    }
    
}

extension Int {
    
    func asInt32() -> Int32 {
        return Int32(clamping: self)
    }
    
    func asKt() -> KotlinInt {
        return KotlinInt(int: self.asInt32())
    }
    
    func formattedWithDecimalSeparator() -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        return numberFormatter.string(from: NSNumber(value: self)) ?? "\(self)"
    }
    
    func scaled(_ appContext: AppContext) -> Int {
        return Int(appContext.screenScale.rounded()) * self
    }
    
}

extension Bool {
    
    func asKt() -> KotlinBoolean {
        return KotlinBoolean(bool: self)
    }
    
}

extension Float {
    
    func scaled(_ appContext: AppContext) -> Float {
        return appContext.screenScale * self
    }
    
}

extension Double {
    
    func scaled(_ appContext: AppContext) -> Double {
        return Double(appContext.screenScale) * self
    }
    
}

extension String {
    
    func asNs() -> NSString {
        return NSString(string: self)
    }
    
    func getKMBSubsidiary() -> KMBSubsidiary {
        return RouteExtensionsKt.getKMBSubsidiary(self)
    }
    
}
