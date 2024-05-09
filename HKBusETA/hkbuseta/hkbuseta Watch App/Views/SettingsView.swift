//
//  ContentView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import WatchConnectivity
import shared

struct SettingsView: AppScreenView {
    
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()
    
    private let appContext: AppActiveContextWatchOS
    
    @State private var historyEnabled = Shared().historyEnabled
    @State private var etaDisplayMode = Shared().etaDisplayMode
    @State private var disableMarquee = Shared().disableMarquee
    
    @State private var phoneConnection = WearableConnectionState.noneDetected
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
    }
    
    func relaunch() {
        let data = newAppDataConatiner()
        data["relaunch"] = AppScreen.settings
        let intent = newAppIntent(appContext, AppScreen.main, data)
        intent.addFlags(flags: [AppIntentFlag.theNewTask, AppIntentFlag.clearTask, AppIntentFlag.noAnimation].asKt())
        appContext.startActivity(appIntent: intent)
        appContext.finishAffinity()
    }

    func invalidatePhoneCache() {
        if (WCSession.default.isReachable) {
            let payload = ["messageType": Shared().INVALIDATE_CACHE_ID]
            WCSession.default.sendMessage(payload) { _ in }
        }
    }
    
    var body: some View {
        ScrollView(.vertical) {
            LazyVStack {
                Spacer().frame(height: 25.0.scaled(appContext))
                SettingsButton(
                    onClick: {
                        registry(appContext).setLanguage(language: Shared().language == "en" ? "zh" : "en", context: appContext)
                        relaunch()
                    },
                    icon: { (Shared().language == "en" ? Image("character.textbox.en") : Image(systemName: "character.textbox.zh"), true) } ,
                    text: "切換語言 Switch Language".asAttributedString(),
                    subText: (Shared().language == "en" ? "English/中文" : "中文/English").asAttributedString()
                )
                SettingsButton(
                    onClick: {
                        registryInvalidateCache(appContext)
                        registryClear()
                        invalidatePhoneCache()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            relaunch()
                        }
                    },
                    icon: { (Image(systemName: "arrow.triangle.2.circlepath"), true) },
                    text: (Shared().language == "en" ? "Update Route Database" : "更新路線資料庫").asAttributedString(),
                    subText: ("\(Shared().language == "en" ? "Last updated" : "最近更新時間"): \(registry(appContext).getLastUpdatedTime() != nil ? (appContext.formatDateTime(localDateTime: TimeUtilsKt.toLocalDateTime(registry(appContext).getLastUpdatedTime()!.int64Value), includeTime: true)) : (Shared().language == "en" ? "Never" : "從未"))").asAttributedString()
                )
                SettingsButton(
                    onClick: {
                        registry(appContext).setHistoryEnabled(historyEnabled: !Shared().historyEnabled, context: appContext)
                        historyEnabled = Shared().historyEnabled
                    },
                    icon: { (Image(systemName: historyEnabled ? "clock.arrow.circlepath" : "clock.badge.xmark"), true) },
                    text: (Shared().language == "en" ? "Recent History" : "歷史記錄").asAttributedString(),
                    subText: (historyEnabled ? (Shared().language == "en" ? "Enabled" : "開啟") : (Shared().language == "en" ? "Disabled" : "停用")).asAttributedString()
                )
                SettingsButton(
                    onClick: {
                        registry(appContext).setEtaDisplayMode(etaDisplayMode: Shared().etaDisplayMode.next, context: appContext)
                        etaDisplayMode = Shared().etaDisplayMode
                    },
                    icon: {
                        switch etaDisplayMode {
                        case .countdown:
                            return (Image(systemName: "timer"), true)
                        case .clockTime:
                            return (Image(systemName: "clock"), true)
                        default:
                            return (Image(systemName: "arrow.triangle.2.circlepath"), true)
                        }
                    },
                    text: (Shared().language == "en" ? "Clock Time Display Mode" : "時間顯示模式").asAttributedString(),
                    subText: {
                        switch etaDisplayMode {
                        case .countdown:
                            return Shared().language == "en" ? "Countdown" : "倒數時間"
                        case .clockTime:
                            return Shared().language == "en" ? "Clock Time" : "時鐘時間"
                        default:
                            return Shared().language == "en" ? "Clock Time + Countdown" : "時鐘+倒數時間"
                        }
                    }()
                )
                SettingsButton(
                    onClick: {
                        registry(appContext).setDisableMarquee(disableMarquee: !Shared().disableMarquee, context: appContext)
                        disableMarquee = Shared().disableMarquee
                    },
                    icon: { (Image(systemName: "textformat.abc.dottedunderline"), true) },
                    text: (Shared().language == "en" ? "Text Marquee Mode" : "文字顯示模式").asAttributedString(),
                    subText: (disableMarquee ? (Shared().language == "en" ?  "Disable Text Marquee" : "靜止模式") : (Shared().language == "en" ?  "Enable Text Marquee" : "走馬燈模式")).asAttributedString()
                )
                switch phoneConnection {
                case .connected:
                    SettingsButton(
                        icon: { (Image(systemName: "iphone"), true) },
                        text: (Shared().language == "en" ? "Mobile Sync" : "手機同步").asAttributedString(),
                        subText: (Shared().language == "en" ? "Connected ✓" : "已連接 ✓").asAttributedString()
                    )
                case .paired:
                    SettingsButton(
                        icon: { (Image(systemName: "iphone"), true) },
                        text: (Shared().language == "en" ? "Mobile Sync" : "手機同步").asAttributedString(),
                        subText: (Shared().language == "en" ? "Paired (Mobile App in the Background)" : "已配對 (手機程式在後台)").asAttributedString()
                    )
                default:
                    SettingsButton(
                        onClick: appContext.handleWebpages(url: Shared_watchosKt.BASE_URL, longClick: false, haptics: hapticsFeedback()),
                        icon: { (Image(systemName: "iphone"), true) },
                        text: (Shared().language == "en" ? "Mobile App" : "手機應用程式").asAttributedString()
                    )
                }
                SettingsButton(
                    onClick: appContext.handleWebpages(url: Shared_watchosKt.BASE_URL, longClick: false, haptics: hapticsFeedback()),
                    icon: { (Image(systemName: "square.and.arrow.up"), true) },
                    text: (Shared().language == "en" ? "Share App" : "分享應用程式").asAttributedString()
                )
                SettingsButton(
                    onClick: appContext.handleWebpages(url: "https://data.hkbuseta.com/PRIVACY_POLICY.html", longClick: false, haptics: hapticsFeedback()),
                    icon: { (Image(systemName: "person.fill"), true) },
                    text: (Shared().language == "en" ? "Privacy Policy" : "隱私權聲明").asAttributedString()
                )
                SettingsButton(
                    onClick:  appContext.handleWebpages(url: "https://apps.apple.com/app/id6475241017", longClick: false, haptics: hapticsFeedback()),
                    onLongClick: appContext.handleWebpages(url: "https://loohpjames.com", longClick: true, haptics: hapticsFeedback()),
                    icon: { (Image("icon_circle"), false) },
                    text: "\(Shared().language == "en" ? "HK Bus ETA" : "香港巴士到站預報") v\(appContext.versionName) (\(appContext.versionCode.description))".asAttributedString(),
                    subText: "@LoohpJames".asAttributedString()
                )
                Spacer().frame(height: 25.0.scaled(appContext))
            }
        }
        .onReceive(timer) { _ in
            if WCSession.default.isReachable {
                phoneConnection = WearableConnectionState.connected
            } else if WCSession.default.isCompanionAppInstalled {
                phoneConnection = WearableConnectionState.paired
            } else {
                phoneConnection = WearableConnectionState.noneDetected
            }
        }
    }
    
    func SettingsButton(
        onClick: @escaping () -> Void = { /* do nothing */ },
        onLongClick: @escaping () -> Void = { /* do nothing */ },
        icon: @escaping () -> (Image, Bool),
        text: AttributedString,
        subText: AttributedString? = nil
    ) -> some View {
        return Button(action: { /* do nothing */ }) {
            HStack(alignment: .center, spacing: 2.scaled(appContext)) {
                ZStack(alignment: .topLeading) {
                    Text("").frame(maxHeight: .infinity)
                    ZStack {
                        Circle()
                            .fill(Color(red: 61/255, green: 61/255, blue: 61/255))
                            .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
                        let (image, isIcon) = icon()
                        if isIcon {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                                .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        } else {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 17.scaled(appContext, true), height: 17.scaled(appContext, true))
                        }
                    }
                }.frame(maxHeight: .infinity)
                VStack(spacing: 0) {
                    Text(text)
                        .font(.system(size: 16.scaled(appContext, true)))
                        .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                        .lineSpacing(0)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                    if subText != nil {
                        Text(subText!)
                            .font(.system(size: 12.scaled(appContext, true)))
                            .foregroundColor(colorInt(0xFFFFFFFF).asColor())
                            .lineSpacing(0)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(10.scaled(appContext))
        }
        .buttonStyle(PlainButtonStyle())
        .background { colorInt(0xFF1A1A1A).asColor() }
        .frame(width: 178.0.scaled(appContext))
        .frame(minHeight: 47.0.scaled(appContext, true))
        .clipShape(RoundedRectangle(cornerRadius: 23.5.scaled(appContext)))
        .padding(.vertical, 1.scaled(appContext))
        .simultaneousGesture(
            LongPressGesture()
                .onEnded { _ in
                    playHaptics()
                    onLongClick()
                }
        )
        .highPriorityGesture(
            TapGesture()
                .onEnded { _ in
                    onClick()
                }
        )
    }
}
