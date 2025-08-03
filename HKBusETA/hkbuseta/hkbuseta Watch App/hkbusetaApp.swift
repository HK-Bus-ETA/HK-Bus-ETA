//
//  hkbusetaApp.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared
import Gzip
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import WidgetKit
import WatchKit
import WatchConnectivity

class ApplicationDelegate: NSObject, WKApplicationDelegate, WCSessionDelegate, UNUserNotificationCenterDelegate {
    
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
    
    func applicationDidFinishLaunching() {
        FirebaseApp.configure()
        
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { _, _ in }
        )
        WKApplication.shared().registerForRemoteNotifications()
    }
    
    func session(_ session: WCSession, didReceiveMessage payload: [String: Any]) {
        handleDataFromPhone(payload: payload)
    }
    
    func session(_ session: WCSession, didReceiveMessage payload: [String : Any], replyHandler: @escaping ([String : Any]) -> Void) {
        handleDataFromPhone(payload: payload)
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext payload: [String : Any]) {
        handleDataFromPhone(payload: payload)
    }
    
    func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        print("Registered for Apple Remote Notifications")
        Messaging.messaging().setAPNSToken(deviceToken, type: .unknown)
        
        Messaging.messaging().subscribe(toTopic: "General") { error in
            print("Subscribed to General")
        }
        Messaging.messaging().subscribe(toTopic: "Refresh") { error in
            print("Subscribed to Refresh")
        }
    }
    
    func didReceiveRemoteNotification(_ userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (WKBackgroundFetchResult) -> Void) {
        print("\(userInfo)")
        if let action = userInfo["action"] as? String, action == "Refresh" {
            AppContextWatchOSKt.runDailyUpdate { completionHandler(.newData) }
        } else {
            completionHandler(.newData)
        }
    }
    
}

@main
struct hkbuseta_Watch_AppApp: App {
    
    @WKApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate
    
    @Environment(\.scenePhase) var scenePhase
    
    @StateObject private var historyStackState = StateFlowListObservable(stateFlow: HistoryStack().historyStack, initSubscribe: true)
    @State private var historyStackLocal: [Int64] = []
    
    @StateObject private var globalWritingFilesCounterState = StateFlowObservable(stateFlow: AppContextKt.globalWritingFilesCounterState, initSubscribe: true)
    @State private var globalWritingFiles = false
    
    let toastTimer = Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()
    @State private var toastText = ""
    @State private var toastShowTimeLeft = 0.0
    
    @State private var watchDataOverwriteWarning = AppContextWatchOSKt.isNewInstall(context: applicationContext())
    @State private var requestSent = false
    
    @State private var alightReminderRequested = false
    
    @StateObject private var toastState = StateFlowObservable(stateFlow: ToastTextState().toastState, initSubscribe: true)
    
    init() {
        initImplementations()
    }
    
    private func initImplementations() {
        HttpResponseUtils_watchosKt.provideGzipBodyAsTextImpl(impl: { data, charset in
            let decompressedData: Data = data.isGzipped ? try! data.gunzipped() : data
            return String(data: decompressedData, encoding: encoding(from: charset))!
        })
        AppContextWatchOSKt.setTilesUpdateImpl {
            WidgetCenter.shared.invalidateConfigurationRecommendations()
            WidgetCenter.shared.reloadAllTimelines()
        }
        AppContextWatchOSKt.setOpenMapsImpl(handler: { lat, lng, label, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openMaps(lat: Double(truncating: lat), lng: Double(truncating: lng), label: label, longClick: Bool(truncating: longClick))
        })
        AppContextWatchOSKt.setOpenWebpagesImpl(handler: { url, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openUrl(link: url, longClick: Bool(truncating: longClick))
        })
        AppContextWatchOSKt.setOpenImagesImpl(handler: { url, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openUrl(link: url, longClick: Bool(truncating: longClick))
        })
        AppContextWatchOSKt.setSyncPreferencesImpl { preferenceJson in
            do {
                let payload = [
                    "messageType": Shared().SYNC_PREFERENCES_ID,
                    "payload": preferenceJson
                ]
                if WCSession.default.isReachable {
                    WCSession.default.sendMessage(payload) { _ in }
                } else {
                    try WCSession.default.updateApplicationContext(payload)
                }
            } catch {
            }
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if watchDataOverwriteWarning {
                    VStack {
                        Text("與手機數據同步中...")
                            .font(.system(size: min(17.scaled(applicationContext(), true), 21.scaled(applicationContext()))))
                        Text("Syncing with Phone...")
                            .font(.system(size: min(17.scaled(applicationContext(), true), 21.scaled(applicationContext()))))
                    }
                } else {
                    NavigationStack(path: $historyStackLocal) {
                        ZStack { /* do nothing */ }
                            .navigationDestination(for: Int64.self) { id in
                                if let context = historyStackState.state.first(where: { $0.activeContextId == id }) {
                                    ZStack {
                                        context.screen.newView(context: context)
                                        TopOverlayBar(context)
                                        if toastShowTimeLeft > 0 {
                                            Text(toastText)
                                                .multilineTextAlignment(.center)
                                                .autoResizing(maxSize: 17.scaled(context, true))
                                                .padding(10.scaled(context))
                                                .background(colorInt(0xFF333333).asColor())
                                                .cornerRadius(10.scaled(context))
                                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                                .background(Color.clear)
                                                .ignoresSafeArea(.all)
                                                .transition(.opacity.animation(.linear(duration: 0.4)))
                                                .allowsHitTesting(false)
                                                .offset(y: Double(context.screenHeight) * 0.25)
                                                .zIndex(1)
                                        }
                                    }
                                    .navigationBarBackButtonHidden(!context.screen.needBackButton())
                                } else {
                                    ZStack { /* do nothing */ }
                                        .navigationBarBackButtonHidden(true)
                                }
                            }
                    }
                }
            }
            .onAppear {
                historyStackLocal = historyStackState.state.map { $0.activeContextId }
                if watchDataOverwriteWarning {
                    watchDataOverwriteWarning = watchDataOverwriteWarning && WCSession.default.isCompanionAppInstalled
                }
                alightReminderRequested = false
            }
            .onChange(of: scenePhase) { newPhase in
                if newPhase == .active {
                    alightReminderRequested = false
                }
            }
            .onChange(of: toastState.state) { state in
                if !state.text.isEmpty {
                    self.toastText = state.text
                    self.toastShowTimeLeft = state.duration.seconds()
                }
            }
            .onChange(of: historyStackState.state) { historyStack in
                historyStackLocal = historyStack.map { $0.activeContextId }
                if let context = historyStack.last, context.screen.needEnsureRegistryDataAvailable() {
                    Shared().ensureRegistryDataAvailable(context: context)
                }
            }
            .onChange(of: historyStackLocal) { localHistoryStack in
                if Array(historyStackState.state.dropLast()).map({ $0.activeContextId }) == localHistoryStack {
                    HistoryStack().popHistoryStack()
                }
            }
            .onChange(of: globalWritingFilesCounterState.state) { globalWritingFilesCounter in
                if globalWritingFilesCounter.intValue > 0 {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        if globalWritingFilesCounterState.state.intValue > 0 {
                            globalWritingFiles = true
                        }
                    }
                } else {
                    globalWritingFiles = false
                }
            }
            .onReceive(toastTimer) { _ in
                if self.toastShowTimeLeft > 0 {
                    self.toastShowTimeLeft -= 0.5
                } else if !self.toastText.isEmpty {
                    ToastTextState().resetToastState()
                }
                if watchDataOverwriteWarning {
                    watchDataOverwriteWarning = AppContextWatchOSKt.isNewInstall(context: applicationContext())
                    if !requestSent {
                        requestSent = applicationContext().requestPreferencesIfPossible().getCompleted() as! Bool
                    }
                }
                if !alightReminderRequested {
                    let payload = ["messageType": Shared().REQUEST_ALIGHT_REMINDER_ID]
                    if WCSession.default.isReachable {
                        WCSession.default.sendMessage(payload) { _ in }
                        alightReminderRequested = true
                    }
                }
            }
            .onOpenURL { url in
                if let context = historyStackState.state.last {
                    if url.absoluteString == "hkbuseta://tileswidget" {
                        let data = newAppDataConatiner()
                        data["launch"] = "etaTile"
                        context.startActivity(appIntent: newAppIntent(context, AppScreen.main, data))
                        context.finishAffinity()
                    } else {
                        AppContextWatchOSKt.extractAndLaunchShareLink(url: url.absoluteString, instance: context, noAnimation: false, skipTitle: false)
                    }
                }
            }
        }
    }
    
    @ViewBuilder func TopOverlayBar(_ appContext: AppActiveContextWatchOS) -> some View {
        if appContext.screen.needBackButton() || globalWritingFiles {
            ZStack {
                if globalWritingFiles {
                    Circle()
                        .fill(colorInt(0xFF00FF00).asColor())
                        .frame(width: 10.scaled(appContext), height: 10.scaled(appContext))
                        .position(x: 46.scaled(appContext), y: 22.5.scaled(appContext))
                        .transition(.opacity.animation(.linear(duration: 0.3)))
                        .zIndex(2)
                }
            }
            .background(alignment: .top) {
                if appContext.screen.isScrollingScreen() {
                    LinearGradient(gradient: Gradient(colors: [colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0x00000000).asColor()]), startPoint: .top, endPoint: .bottom)
                        .frame(height: 45.scaled(appContext))
                }
            }
            .frame(
                maxWidth: .infinity,
                maxHeight: .infinity,
                alignment: .top
            )
            .ignoresSafeArea(.all)
        }
    }
}

func handleDataFromPhone(payload: [String: Any]) {
    let type = payload["messageType"] as! String
    switch type {
    case Shared().START_ACTIVITY_ID:
        let queryKey = payload["k"] as? String
        let queryRouteNumber = payload["r"] as? String
        let queryBound = payload["b"] as? String
        let queryCoRaw = payload["c"] as? String
        let queryCo = queryCoRaw == nil ? nil as Operator? : Operator.Companion().valueOf(name: queryCoRaw!)
        let queryDest = payload["d"] as? String
        let queryGMBRegionRaw = payload["g"] as? String
        let queryGMBRegion = queryGMBRegionRaw == nil ? nil as GMBRegion? : GMBRegion.Companion().valueOfOrNull(name: queryGMBRegionRaw!)
        let queryStop = payload["s"] as? String
        let queryStopIndex = (payload["si"] as? Int ?? 0).asInt32()
        let queryStopDirectLaunch = payload["sd"] as? Bool ?? false
        Shared().handleLaunchOptions(instance: HistoryStack().historyStack.value.first! as! AppActiveContextWatchOS, stopId: nil, co: nil, index: nil, stop: nil, route: nil, listStopRoute: nil, listStopScrollToStop: nil, listStopShowEta: nil, queryKey: queryKey, queryRouteNumber: queryRouteNumber, queryBound: queryBound, queryCo: queryCo, queryDest: queryDest, queryGMBRegion: queryGMBRegion, queryStop: queryStop, queryStopIndex: queryStopIndex, queryStopDirectLaunch: queryStopDirectLaunch, appScreen: nil, noAnimation: false, skipTitle: false, orElse: { })
    case Shared().SYNC_PREFERENCES_ID:
        let preferences = payload["payload"] as! String
        AppContextWatchOSKt.syncPreference(context: applicationContext(), preferenceJson: preferences, sync: false)
    case Shared().REQUEST_PREFERENCES_ID:
        let payload: [String: Any] = [
            "messageType": Shared().SYNC_PREFERENCES_ID,
            "payload": AppContextWatchOSKt.getRawPreference(context: applicationContext())
        ]
        WCSession.default.sendMessage(payload) { _ in }
    case Shared().UPDATE_ALIGHT_REMINDER_ID:
        let payload = payload["payload"] as! String
        let json = payload.isEmpty ? nil : payload
        AppContextWatchOSKt.receiveAlightReminderRemoteData(launch: false, json: json)
    case Shared().RESPONSE_ALIGHT_REMINDER_ID:
        let payload = payload["payload"] as! String
        let json = payload.isEmpty ? nil : payload
        AppContextWatchOSKt.receiveAlightReminderRemoteData(launch: true, json: json)
    case Shared().INVALIDATE_CACHE_ID:
        registryInvalidateCache(applicationContext())
        registryClear()
        let _ = registry(applicationContext())
        let intent = newAppIntent(applicationContext(), AppScreen.main)
        intent.addFlags(flags: [AppIntentFlag.theNewTask].asKt())
        applicationContext().startActivity(appIntent: intent)
    default:
        { /* do nothing */ }()
    }
}

extension View {
    
    @ViewBuilder func defaultStyle() -> some View {
        self.background { colorInt(0xFF000000).asColor() }
    }
    
}

extension AppScreen {
    
    @ViewBuilder func newView(context: AppActiveContextWatchOS) -> some View {
        switch self {
        case AppScreen.dummy:
            DummyView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.fatalError:
            FatalErrorView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.main:
            MainView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.title:
            TitleView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.search:
            SearchView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.nearby:
            NearbyView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.listRoutes:
            ListRoutesView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.listStops:
            ListStopsView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.eta:
            EtaView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.etaMenu:
            EtaMenuView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.fav:
            FavView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.favRouteListView:
            FavRouteListViewView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.settings:
            SettingsView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.etaTileList:
            EtaTileListView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.etaTileConfigure:
            EtaTileConfigurationView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.alightReminderService:
            AlightReminderView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.searchTrain:
            TrainRouteMapView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        default:
            MainView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        }
    }
    
    func needBackButton() -> Bool {
        switch self {
        case AppScreen.main, AppScreen.title, AppScreen.dummy, AppScreen.fatalError:
            return false
        default:
            return true
        }
    }
    
    func isScrollingScreen() -> Bool {
        switch self {
        case AppScreen.listStops, AppScreen.listRoutes, AppScreen.fav, AppScreen.etaMenu, AppScreen.etaTileList, AppScreen.etaTileConfigure, AppScreen.settings:
            return true
        default:
            return false
        }
    }
    
    func needEnsureRegistryDataAvailable() -> Bool {
        switch self {
        case AppScreen.eta, AppScreen.etaMenu, AppScreen.fav, AppScreen.favRouteListView, AppScreen.listRoutes, AppScreen.listStops, AppScreen.nearby, AppScreen.search, AppScreen.title:
            return true
        default:
            return false
        }
    }
    
}

extension ToastDuration {
    
    func seconds() -> Double {
        switch self {
        case ToastDuration.short_:
            return 3.0
        case ToastDuration.long_:
            return 6.0
        default:
            return 3.0
        }
    }
    
}
