//
//  MainView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI

struct MainView: View {
    var body: some View {
        VStack {
            Image("icon_full")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 60.0, height: 60.0)
                .padding(.all)
            Text("載入中...").padding(.top)
            Text("Loading...").padding(.bottom)
        }
    }
}

#Preview {
    MainView()
}
