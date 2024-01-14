//
//  BaseView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 14/01/2024.
//

import SwiftUI
import shared

protocol AppScreenView: View {
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>)
    
}
