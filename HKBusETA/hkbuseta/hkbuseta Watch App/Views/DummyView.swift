//
//  DummyView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 23/12/2023.
//

import SwiftUI
import shared

struct DummyView: AppScreenView {

    private let appContext: AppActiveContextWatchOS

    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            appContext.finish()
        }
    }

    var body: some View {
        Text("")
    }
}
