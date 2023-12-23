//
//  hkbusetaApp.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

@main
struct hkbuseta_Watch_AppApp: App {
    
    @StateObject private var historyStackState = FlowStateObservable(defaultValue: appContext().historyStack, nativeFlow: appContext().historyStackFlow)
    
    var body: some Scene {
        WindowGroup {
            let item = historyStackState.state.last!
            switch item.screen {
            case AppScreen.dummy:
                DummyView(data: item.data).defaultStyle()
            case AppScreen.main:
                MainView(data: item.data).defaultStyle()
            case AppScreen.title:
                TitleView(data: item.data).defaultStyle()
            case AppScreen.search:
                SearchView(data: item.data).defaultStyle()
            case AppScreen.listRoutes:
                ListRoutesView(data: item.data).defaultStyle()
            case AppScreen.listStops:
                ListStopsView(data: item.data).defaultStyle()
            case AppScreen.eta:
                EtaView(data: item.data).defaultStyle()
            default:
                MainView(data: item.data).defaultStyle()
            }
        }
    }
}

extension View {
    
    func defaultStyle() -> some View {
        return self
            .transition(AnyTransition.scale.animation(.easeInOut(duration: 0.25)))
            .background { 0xFF000000.asColor() }
    }
    
}
