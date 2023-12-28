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
    
    @StateObject private var registryState = FlowStateObservable(defaultValue: registry().state, nativeFlow: registry().stateFlow)
    
    @StateObject private var updateProgressState = FlowStateObservable(defaultValue: KotlinFloat(value: registry().updatePercentageState), nativeFlow: registry().updatePercentageStateFlow)
    
    @State private var updateScreen = false
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        
    }
    
    var body: some View {
        VStack {
            if updateScreen {
                Text("更新數據中...")
                    .font(.system(size: 23.scaled()))
                Text("更新需時 請稍等")
                    .font(.system(size: 14.scaled()))
                    .padding(.bottom)
                Text("Updating...")
                    .font(.system(size: 23.scaled()))
                Text("Might take a moment")
                    .font(.system(size: 14.scaled()))
                    .padding(.bottom)
                ProgressView(value: max(0.0, min(1.0, updateProgressState.state.floatValue)))
                    .padding(.top)
                    .frame(width: 150.0.scaled())
                    .animation(.easeInOut(duration: 0.2), value: updateProgressState.state.floatValue)
            } else {
                Image("icon_full")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 60.0.scaled(), height: 60.0.scaled())
                Text("載入中...")
                    .font(.system(size: 23.scaled()))
                    .padding(.top)
                Text("Loading...")
                    .font(.system(size: 23.scaled()))
                    .padding(.bottom)
            }
        }.onChange(of: registryState.state) {
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    appContext().appendStack(screen: AppScreen.title)
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            }
        }.onAppear {
            appContext().clearStack()
            if registryState.state == Registry.State.ready {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    appContext().appendStack(screen: AppScreen.title)
                }
            } else if registryState.state == Registry.State.updating {
                updateScreen = true
            }
        }
    }
}

#Preview {
    MainView(data: [:], storage: KotlinMutableDictionary())
}
