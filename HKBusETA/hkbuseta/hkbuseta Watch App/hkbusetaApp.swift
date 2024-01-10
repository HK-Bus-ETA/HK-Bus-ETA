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

class ApplicationDelegate: NSObject, WKApplicationDelegate {
    
  func applicationDidFinishLaunching() {
      FirebaseApp.configure()
  }
    
}

@main
struct hkbuseta_Watch_AppApp: App {
    
    @WKApplicationDelegateAdaptor(ApplicationDelegate.self) var delegate
    
    @StateObject private var historyStackState = FlowStateObservable(defaultValue: HistoryStack().historyStack, nativeFlow: HistoryStack().historyStackFlow)
    
    init() {
        HttpResponseUtils_watchosKt.provideGzipBodyAsTextImpl(impl: { data, charset in
            let decompressedData: Data = data.isGzipped ? try! data.gunzipped() : data
            return String(data: decompressedData, encoding: encoding(from: charset))!
        })
        AppContextWatchOSKt.setOpenMapsImpl(handler: { lat, lng, label, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openMaps(lat: Double(truncating: lat), lng: Double(truncating: lng), label: label)
        })
        AppContextWatchOSKt.setOpenWebpagesImpl(handler: { url, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openUrl(link: url)
        })
        AppContextWatchOSKt.setOpenImagesImpl(handler: { url, longClick, haptics in
            if Bool(truncating: longClick) {
                haptics.performHapticFeedback(hapticFeedbackType: HapticFeedbackType.longpress)
            }
            openUrl(link: url)
        })
    }
    
    var body: some Scene {
        WindowGroup {
            let context = historyStackState.state.last!
            ZStack {
                switch context.screen {
                case AppScreen.dummy:
                    DummyView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
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
                default:
                    MainView(appContext: context, data: context.data, storage: context.storage).defaultStyle()
                }
                if context.screen.needBackButton() {
                    BackButton(context, scrollingScreen: context.screen.isScrollingScreen())
                }
            }
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
        .frame(width: 30.scaled(appContext), height: 30.scaled(appContext))
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

extension View {
    
    func defaultStyle() -> some View {
        return self
            .transition(AnyTransition.scale.animation(.easeInOut(duration: 0.25)))
            .background { colorInt(0xFF000000).asColor() }
    }
    
}

extension AppScreen {
    
    func needBackButton() -> Bool {
        switch self {
        case AppScreen.main, AppScreen.title:
            return false
        default:
            return true
        }
    }
    
    func isScrollingScreen() -> Bool {
        switch self {
        case AppScreen.listStops, AppScreen.listRoutes, AppScreen.fav, AppScreen.etaMenu:
            return true
        default:
            return false
        }
    }
    
}
