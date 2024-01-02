//
//  ContentView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

struct TitleView: View {
    
    let appVersion: String = appContext().versionName
    let buildVersion: String = appContext().versionCode.description
    
    init(data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        
    }
    
    var body: some View {
        VStack {
            Spacer().frame(height: 25.0.scaled())
            Button(action: {
                appContext().appendStack(screen: AppScreen.search)
            }) {
                Text(Shared().language == "en" ? "Input Route" : "輸入巴士路線")
                    .font(.system(size: 20.scaled(), weight: .bold))
            }
            .background(
                Image("bus_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(), height: 53.0.scaled())
                    .brightness(/*@START_MENU_TOKEN@*/-0.2/*@END_MENU_TOKEN@*/)
                    .clipShape(RoundedRectangle(cornerRadius: 50))
            )
            .frame(width: 178.0.scaled(), height: 53.0.scaled())
            .clipShape(RoundedRectangle(cornerRadius: 50))
            Button(action: {
                appContext().appendStack(screen: AppScreen.nearby)
            }) {
                Text(Shared().language == "en" ? "Search Nearby" : "附近巴士路線")
                    .font(.system(size: 20.scaled(), weight: .bold))
            }.background(
                Image("nearby_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(), height: 53.0.scaled())
                    .brightness(/*@START_MENU_TOKEN@*/-0.4/*@END_MENU_TOKEN@*/)
                    .clipShape(RoundedRectangle(cornerRadius: 50))
            )
            .frame(width: 178.0.scaled(), height: 53.0.scaled())
            .clipShape(RoundedRectangle(cornerRadius: 50))
            HStack {
                Button(action: {
                    registry().setLanguage(language: Shared().language == "en" ? "zh" : "en", context: appContext())
                    appContext().appendStack(screen: AppScreen.main)
                }) {
                    Text(Shared().language == "en" ? "中文" : "English")
                        .font(.system(size: 20.scaled(), weight: .bold))
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(width: 115.scaled())
                Button(action: {
                    appContext().appendStack(screen: AppScreen.fav)
                }) {
                    Image(systemName: "star.fill").font(.system(size: 21.scaled())).foregroundColor(.yellow)
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(width: 55.scaled())
            }
            Text(Shared().language == "en" ? "HK Bus ETA" : "香港巴士到站預報").font(.system(size: 12.scaled()))
            Text("v\(appVersion) (\(buildVersion)) @loohpjames").font(.system(size: 12.scaled())).padding(.bottom)
        }
        .padding()
    }
}

#Preview {
    TitleView(data: [:], storage: KotlinMutableDictionary())
}
