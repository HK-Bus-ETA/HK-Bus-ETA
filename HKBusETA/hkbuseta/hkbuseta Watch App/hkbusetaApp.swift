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
import WatchKit
import WatchConnectivity

class ApplicationDelegate: NSObject, WKApplicationDelegate, WCSessionDelegate {
    
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
    }
    
    func session(_ session: WCSession, didReceiveMessage payload: [String: Any]) {
        handleLaunchOptions(payload: payload)
    }
    
    func session(_ session: WCSession, didReceiveMessage payload: [String : Any], replyHandler: @escaping ([String : Any]) -> Void) {
        handleLaunchOptions(payload: payload)
    }
    
}

@main
struct hkbuseta_Watch_AppApp: App {
    
    @WKApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate
    
    @StateObject private var historyStackState = FlowStateObservable(defaultValue: HistoryStack().historyStack, nativeFlow: HistoryStack().historyStackFlow)
    
    let toastTimer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()
    @State private var toastText = ""
    @State private var toastShowTimeLeft = 0.0
    
    @StateObject private var toastState = FlowStateObservable(defaultValue: ToastTextState().toastState, nativeFlow: ToastTextState().toastStateFlow)
    
    init() {
        initImplementations()
    }
    
    private func initImplementations() {
        HttpResponseUtils_watchosKt.provideGzipBodyAsTextImpl(impl: { data, charset in
            let decompressedData: Data = data.isGzipped ? try! data.gunzipped() : data
            return String(data: decompressedData, encoding: encoding(from: charset))!
        })
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
    }
    
    var body: some Scene {
        WindowGroup {
            let context = historyStackState.state.last!
            ZStack {
                context.screen.newView(context: context)
                if context.screen.needBackButton() {
                    BackButton(context, scrollingScreen: context.screen.isScrollingScreen())
                }
                if toastShowTimeLeft > 0 {
                    Text(toastText)
                        .multilineTextAlignment(.center)
                        .autoResizing(maxSize: 17.scaled(applicationContext()))
                        .padding(10.scaled(applicationContext()))
                        .background(colorInt(0xFF333333).asColor())
                        .cornerRadius(10.scaled(applicationContext()))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.clear)
                        .edgesIgnoringSafeArea(.all)
                        .transition(.opacity.animation(.linear(duration: 0.4)))
                        .allowsHitTesting(false)
                        .zIndex(1)
                }
            }
            .onChange(of: toastState.state) { state in
                self.toastText = state.text
                self.toastShowTimeLeft = state.duration.seconds()
            }
            .onChange(of: historyStackState.state) { historyStack in
                let context = historyStack.last!
                if context.screen.needEnsureRegistryDataAvailable() {
                    Shared().ensureRegistryDataAvailable(context: context)
                }
            }
            .onReceive(toastTimer) { _ in
                if self.toastShowTimeLeft > 0 {
                    self.toastShowTimeLeft -= 0.1
                }
            }
            .onOpenURL(perform: { url in
                if url.absoluteString == "hkbuseta://tileswidget" {
                    let data = newAppDataConatiner()
                    data["launch"] = "etaTile"
                    context.startActivity(appIntent: newAppIntent(context, AppScreen.main, data))
                    context.finishAffinity()
                }
            })
        }
    }
}

func BackButton(_ appContext: AppContext, scrollingScreen: Bool) -> some View {
    ZStack {
        Button(action: {
            HistoryStack().popHistoryStack()
        }) {
            Image(systemName: "arrow.left")
                .font(.system(size: 17.scaled(appContext), weight: .bold))
                .foregroundColor(.white)
        }
        .frame(width: 40.scaled(appContext), height: 40.scaled(appContext))
        .buttonStyle(PlainButtonStyle())
        .position(x: 23.scaled(appContext), y: 23.scaled(appContext))
    }
    .background(alignment: .top) {
        if scrollingScreen {
            LinearGradient(gradient: Gradient(colors: [colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0xFF000000).asColor(), colorInt(0x00000000).asColor()]), startPoint: .top, endPoint: .bottom)
                .frame(height: 45.scaled(appContext))
        }
    }
    .frame(
        maxWidth: .infinity,
        maxHeight: .infinity,
        alignment: .top
    )
    .edgesIgnoringSafeArea(.all)
}

func handleLaunchOptions(payload: [String: Any]) {
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
    Shared().handleLaunchOptions(instance: HistoryStack().historyStack.first!, stopId: nil, co: nil, index: nil, stop: nil, route: nil, listStopRoute: nil, listStopScrollToStop: nil, listStopShowEta: nil, listStopIsAlightReminder: nil, queryKey: queryKey, queryRouteNumber: queryRouteNumber, queryBound: queryBound, queryCo: queryCo, queryDest: queryDest, queryGMBRegion: queryGMBRegion, queryStop: queryStop, queryStopIndex: queryStopIndex, queryStopDirectLaunch: queryStopDirectLaunch, orElse: { })
}

extension View {
    
    func defaultStyle() -> some View {
        return self
            .transition(.scale.animation(.easeInOut(duration: 0.25)))
            .background { colorInt(0xFF000000).asColor() }
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
        case AppScreen.etaTileList:
            EtaTileListView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
        case AppScreen.etaTileConfigure:
            EtaTileConfigurationView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
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
        case AppScreen.listStops, AppScreen.listRoutes, AppScreen.fav, AppScreen.etaMenu, AppScreen.etaTileList, AppScreen.etaTileConfigure:
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
