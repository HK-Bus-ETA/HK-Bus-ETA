//
//  MainView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI
import shared
import KMPNativeCoroutinesCore
import KMPNativeCoroutinesRxSwift
import KMPNativeCoroutinesAsync
import KMPNativeCoroutinesCombine
import RxSwift

struct MainView: View {
    
    @StateObject private var registryState: FlowStateObservable<Registry.State>
    
    @StateObject private var updateProgressState: FlowStateObservable<KotlinFloat>
    
    @State private var updateScreen = false
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self._registryState = StateObject(wrappedValue: FlowStateObservable(defaultValue: registry(appContext).state, nativeFlow: registry(appContext).stateFlow))
        self._updateProgressState = StateObject(wrappedValue: FlowStateObservable(defaultValue: KotlinFloat(value: registry(appContext).updatePercentageState), nativeFlow: registry(appContext).updatePercentageStateFlow))
    }
    
    var body: some View {
        VStack {
            if updateScreen {
                Text("更新數據中...")
                    .font(.system(size: 23.scaled(appContext)))
                Text("更新需時 請稍等")
                    .font(.system(size: 14.scaled(appContext)))
                    .padding(.bottom)
                Text("Updating...")
                    .font(.system(size: 23.scaled(appContext)))
                Text("Might take a moment")
                    .font(.system(size: 14.scaled(appContext)))
                    .padding(.bottom)
                ProgressView(value: max(0.0, min(1.0, updateProgressState.state.floatValue)))
                    .padding(.top)
                    .frame(width: 150.0.scaled(appContext))
                    .animation(.easeInOut(duration: 0.2), value: updateProgressState.state.floatValue)
            } else {
                Image("icon_full")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 60.0.scaled(appContext), height: 60.0.scaled(appContext))
                Text("載入中...")
                    .font(.system(size: 23.scaled(appContext)))
                    .padding(.top)
                Text("Loading...")
                    .font(.system(size: 23.scaled(appContext)))
                    .padding(.bottom)
            }
        }.onChange(of: registryState.state) { _ in
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                    appContext.finishAffinity()
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            }
        }.onAppear {
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.title))
                    appContext.finishAffinity()
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            }
        }
    }
}
