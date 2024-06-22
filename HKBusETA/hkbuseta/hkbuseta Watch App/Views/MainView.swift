//
//  MainView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI
import shared

struct MainView: AppScreenView {
    
    @StateObject private var registryState: StateFlowObservable<Registry.State>
    
    @StateObject private var updateProgressState: StateFlowObservable<KotlinFloat>
    
    @State private var updateScreen = false
    @State private var launch: String
    @State private var skipSplash: Bool
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.launch = data["launch"] as? String ?? ""
        self.skipSplash = data["skipSplash"] as? Bool ?? false
        self._registryState = StateObject(wrappedValue: StateFlowObservable(stateFlow: registry(appContext).state))
        self._updateProgressState = StateObject(wrappedValue: StateFlowObservable(stateFlow: registry(appContext).updatePercentageState))
    }
    
    var body: some View {
        VStack {
            if skipSplash {
                Text(" ")
            } else if updateScreen {
                Text("更新數據中...")
                    .font(.system(size: min(23.scaled(appContext, true), 26.scaled(appContext))))
                    .padding(.bottom)
                Text("Updating...")
                    .font(.system(size: min(23.scaled(appContext, true), 26.scaled(appContext))))
                    .padding(.bottom)
                ProgressView(value: max(0.0, min(1.0, updateProgressState.state.floatValue)))
                    .tint(colorInt(0xFFF9DE09).asColor())
                    .padding(.top)
                    .frame(width: 150.0.scaled(appContext))
                    .animation(.easeInOut(duration: 0.2), value: updateProgressState.state.floatValue)
            } else {
                Image("icon_full")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 60.0.scaled(appContext), height: 60.0.scaled(appContext))
                Text("載入中...")
                    .font(.system(size: min(23.scaled(appContext, true), 26.scaled(appContext))))
                    .padding(.top)
                Text("Loading...")
                    .font(.system(size: min(23.scaled(appContext, true), 26.scaled(appContext))))
                    .padding(.bottom)
            }
        }
        .onAppear {
            registryState.subscribe()
            updateProgressState.subscribe()
        }
        .onDisappear {
            registryState.unsubscribe()
            updateProgressState.unsubscribe()
        }
        .onChange(of: registryState.state) { _ in
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    let appScreen = appContext.data["relaunch"] as? AppScreen
                    if appScreen != nil {
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                        appContext.startActivity(appIntent: newAppIntent(appContext, appScreen!))
                        appContext.finishAffinity()
                    } else {
                        switch launch {
                        case "etaTile":
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileList))
                            appContext.finishAffinity()
                        default:
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                            appContext.finishAffinity()
                        }
                    }
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            } else if registryState.state == Registry.State.error {
                let data = newAppDataConatiner()
                data["zh"] = "發生錯誤\n請檢查您的網絡連接"
                data["en"] = "Fatal Error\nPlease check your internet connection"
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.fatalError, data))
                appContext.finishAffinity()
            }
        }
        .onAppear {
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    let appScreen = appContext.data["relaunch"] as? AppScreen
                    if appScreen != nil {
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                        appContext.startActivity(appIntent: newAppIntent(appContext, appScreen!))
                        appContext.finishAffinity()
                    } else {
                        switch launch {
                        case "etaTile":
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileList))
                            appContext.finishAffinity()
                        default:
                            appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                            appContext.finishAffinity()
                        }
                    }
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            } else if registryState.state == Registry.State.error {
                let data = newAppDataConatiner()
                data["zh"] = "發生錯誤\n請檢查您的網絡連接"
                data["en"] = "Fatal Error\nPlease check your internet connection"
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.fatalError, data))
                appContext.finishAffinity()
            }
        }
    }
}
