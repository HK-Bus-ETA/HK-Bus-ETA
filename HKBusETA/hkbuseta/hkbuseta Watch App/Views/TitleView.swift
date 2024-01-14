//
//  ContentView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

struct TitleView: AppScreenView {
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        
    }
    
    var body: some View {
        VStack {
            Spacer().frame(height: 25.0.scaled(appContext))
            Button(action: {
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.search))
            }) {
                Text(Shared().language == "en" ? "Input Route" : "輸入巴士路線")
                    .font(.system(size: 20.scaled(appContext), weight: .bold))
            }
            .background(
                Image("bus_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
                    .brightness(/*@START_MENU_TOKEN@*/-0.2/*@END_MENU_TOKEN@*/)
                    .clipShape(RoundedRectangle(cornerRadius: 50))
            )
            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
            .clipShape(RoundedRectangle(cornerRadius: 50))
            Button(action: {
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.nearby))
            }) {
                Text(Shared().language == "en" ? "Search Nearby" : "附近巴士路線")
                    .font(.system(size: 20.scaled(appContext), weight: .bold))
            }.background(
                Image("nearby_background")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
                    .brightness(/*@START_MENU_TOKEN@*/-0.4/*@END_MENU_TOKEN@*/)
                    .clipShape(RoundedRectangle(cornerRadius: 50))
            )
            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
            .clipShape(RoundedRectangle(cornerRadius: 50))
            HStack {
                Button(action: {
                    registry(appContext).setLanguage(language: Shared().language == "en" ? "zh" : "en", context: appContext)
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.main))
                }) {
                    Text(Shared().language == "en" ? "中文" : (belowWidgetVersion ? "EN" : "English"))
                        .font(.system(size: 20.scaled(appContext), weight: .bold))
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(maxWidth: /*@START_MENU_TOKEN@*/.infinity/*@END_MENU_TOKEN@*/)
                Button(action: {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.fav))
                }) {
                    Image(systemName: "star.fill").font(.system(size: 21.scaled(appContext))).foregroundColor(.yellow)
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(width: 55.scaled(appContext))
                if belowWidgetVersion {
                    Button(action: {
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileList))
                        appContext.finishAffinity()
                    }) {
                        Image(systemName: "info.circle.fill").font(.system(size: 21.scaled(appContext))).foregroundColor(.red)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 50))
                    .frame(width: 55.scaled(appContext))
                }
            }
            Text(Shared().language == "en" ? "HK Bus ETA" : "香港巴士到站預報").font(.system(size: 12.scaled(appContext)))
            Text("v\(appContext.versionName) (\(appContext.versionCode.description)) @loohpjames").font(.system(size: 12.scaled(appContext))).padding(.bottom)
        }
        .padding()
    }
}
