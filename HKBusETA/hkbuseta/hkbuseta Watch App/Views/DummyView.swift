//
//  DummyView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 23/12/2023.
//

import SwiftUI
import shared

struct DummyView: View {
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        
    }
    
    var body: some View {
        Text("")
    }
}

#Preview {
    DummyView(data: [:], storage: KotlinMutableDictionary())
}
