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
import WatchConnectivity

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

func registryClear() {
    Registry.Companion().clearInstance()
}

func fetchEta(appContext: AppContext, stopId: String, stopIndex: Int, co: Operator, route: Route, options: Registry.EtaQueryOptions? = nil, callback: @escaping (Registry.ETAQueryResult) -> Void) {
    fetchEta(appContext: appContext, stopId: stopId, stopIndex: stopIndex.asInt32(), co: co, route: route, options: options, callback: callback)
}

func fetchEta(appContext: AppContext, stopId: String, stopIndex: Int32, co: Operator, route: Route, options: Registry.EtaQueryOptions? = nil, callback: @escaping (Registry.ETAQueryResult) -> Void) {
    let pending = registry(appContext).getEta(stopId: stopId, stopIndex: stopIndex, co: co, route: route, context: appContext, options: options)
    pending.onComplete(timeout: Shared().ETA_UPDATE_INTERVAL, unit: Kotlinx_datetimeDateTimeUnit.Companion().MILLISECOND, callback: { result in
        DispatchQueue.main.async {
            callback(result)
        }
    })
}

func playHaptics() {
    hapticsFeedback().performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
}

func hapticsFeedback() -> HapticFeedback {
    return AppContextWatchOSKt.hapticFeedback
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

func operatorColor(_ primaryColor: Int64, _ secondaryColor: Int64? = nil, _ fraction: Float = 0, _ takeIf: (Int64) -> Bool = { _ in true }) -> Int64 {
    if secondaryColor == nil || !takeIf(secondaryColor!) {
        return primaryColor
    }
    return ColorValueUtilsKt.interpolateColor(startColor: primaryColor, endColor: secondaryColor!, fraction: fraction)
}

func openUrl(link: String, longClick: Bool) {
    guard let url = URL(string: link) else {
        return
    }
    if longClick {
        playHaptics()
    }
    let session = ASWebAuthenticationSession(url: url, callbackURLScheme: nil) { _, _ in }
    session.prefersEphemeralWebBrowserSession = true
    session.start()
}

func openMaps(lat: Double, lng: Double, label: String, longClick: Bool) {
    if longClick {
        playHaptics()
    }
    let coordinate = CLLocationCoordinate2DMake(lat, lng)
    let placemark = MKPlacemark(coordinate: coordinate)
    let mapItem = MKMapItem(placemark: placemark)
    mapItem.name = label
    mapItem.timeZone = TimeZone(identifier: "Asia/Hong_Kong")
    mapItem.openInMaps()
}

extension Array where Element: NSObject {
    
    func asKt() -> KotlinArray<Element> {
        return KotlinArray(size: count.asInt32()) { index in self[Int(truncating: index)] }
    }
    
}

extension View {
    
    @ViewBuilder func tileStateBorder(_ state: TileUseState, _ cornerRadius: CGFloat) -> some View {
        switch state {
        case .primary:
            self.overlay {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(colorInt(0x5437FF00).asColor(), lineWidth: 2)
                    .padding(1)
            }
        case .secondary:
            self.overlay {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(colorInt(0x54FFB700).asColor(), lineWidth: 2)
                    .padding(1)
            }
        default:
            self
        }
    }
    
    func CrossfadeText(textList: [AttributedString], state: Int) -> some View {
        Text(textList[state % textList.count])
            .id(textList[state % textList.count])
            .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    func CrossfadeMarqueeText(textList: [AttributedString], state: Int, font: UIFont, startDelay: Double, alignment: Alignment? = nil) -> some View {
        MarqueeText(
            text: textList[state % textList.count],
            font: font,
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
    
    func scaled(_ appContext: AppContext, _ dynamic: Bool = false) -> Int {
        if dynamic {
            return Int((Double(appContext.screenScale) * Double(self)).dynamicSize().rounded())
        } else {
            return Int(appContext.screenScale.rounded()) * self
        }
    }
    
    func dynamicSize() -> Int {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(Int((Double(self) * 1.2).rounded()), Int(UIFontMetrics.default.scaledValue(for: Double(self)).rounded()))
    }
    
}

extension Bool {
    
    func asKt() -> KotlinBoolean {
        return KotlinBoolean(bool: self)
    }
    
}

extension Float {
    
    func scaled(_ appContext: AppContext, _ dynamic: Bool = false) -> Float {
        if dynamic {
            return (appContext.screenScale * self).dynamicSize()
        } else {
            return appContext.screenScale * self
        }
    }
    
    func dynamicSize() -> Float {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(self * 1.2, Float(UIFontMetrics.default.scaledValue(for: Double(self))))
    }
    
}

extension Double {
    
    func scaled(_ appContext: AppContext, _ dynamic: Bool = false) -> Double {
        if dynamic {
            return (Double(appContext.screenScale) * self).dynamicSize()
        } else {
            return Double(appContext.screenScale) * self
        }
    }
    
    func dynamicSize() -> Double {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(self * 1.2, UIFontMetrics.default.scaledValue(for: self))
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
