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

func appContext() -> AppActiveContextWatchOS {
    return AppContextWatchOSKt.appContext
}

func registry() -> Registry {
    return Registry.Companion().getInstance(context: appContext())
}

func playHaptics() {
    WKInterfaceDevice.current().play(.success)
}

func newAppDataConatiner() -> KotlinMutableDictionary<NSString, AnyObject> {
    return AppContextWatchOSKt.createMutableAppDataContainer()
}

func BackButton(predicate: @escaping (HistoryStackEntry) -> Bool) -> some View {
    Button(action: {
        appContext().popStackIfMatches { KotlinBoolean(bool: predicate($0)) }
    }) {
        Image(systemName: "arrow.left")
            .font(.system(size: 17))
            .bold()
            .foregroundColor(.white)
    }
    .frame(width: 30, height: 30)
    .buttonStyle(PlainButtonStyle())
    .position(x: 20, y: 0)
}
