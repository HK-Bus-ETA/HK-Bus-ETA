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
                Text(Shared().language == "en" ? "Search Routes" : "搜尋路線")
                    .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext)), weight: .bold))
                    .background(
                        Image("bus_background")
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
                            .brightness(-0.45)
                            .clipShape(RoundedRectangle(cornerRadius: 50))
                    )
            }
            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
            .clipShape(RoundedRectangle(cornerRadius: 50))
            .buttonStyle(PlainButtonStyle())
            Button(action: {
                appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.nearby))
            }) {
                Text(Shared().language == "en" ? "Nearby Routes" : "附近路線")
                    .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext)), weight: .bold))
                    .background(
                        Image("nearby_background")
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
                            .brightness(-0.45)
                            .clipShape(RoundedRectangle(cornerRadius: 50))
                    )
            }
            .frame(width: 178.0.scaled(appContext), height: 53.0.scaled(appContext))
            .clipShape(RoundedRectangle(cornerRadius: 50))
            .buttonStyle(PlainButtonStyle())
            HStack {
                Button(action: {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.settings))
                }) {
                    if belowWidgetVersion {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext))))
                            .foregroundColor(.white)
                    } else {
                        Text(Shared().language == "en" ? "Settings" : "設定")
                            .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext)), weight: .bold))
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(height: 53.0.scaled(appContext))
                .frame(maxWidth: .infinity)
                Button(action: {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.fav))
                }) {
                    Image(systemName: "star.fill")
                        .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext))))
                        .foregroundColor(.yellow)
                }
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .frame(width: 55.0.scaled(appContext), height: 53.0.scaled(appContext))
                if belowWidgetVersion {
                    Button(action: {
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileList))
                        appContext.finishAffinity()
                    }) {
                        Image(systemName: "info.circle.fill")
                            .font(.system(size: min(21.scaled(appContext, true), 24.scaled(appContext))))
                            .foregroundColor(.red)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 50))
                    .frame(width: 55.0.scaled(appContext), height: 53.0.scaled(appContext))
                }
            }
            Text(Shared().language == "en" ? "HK Bus ETA" : "香港巴士到站預報").font(.system(size: 12.scaled(appContext)))
            Text("v\(appContext.versionName) (\(appContext.versionCode.description)) @loohpjames").font(.system(size: 12.scaled(appContext))).padding(.bottom)
        }
        .padding()
    }
}
