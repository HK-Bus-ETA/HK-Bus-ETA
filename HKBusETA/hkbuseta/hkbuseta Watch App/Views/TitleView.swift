//
//  ContentView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

struct TitleView: AppScreenView {
    
    let alertUpdateTimer = Timer.publish(every: 30, on: .main, in: .common).autoconnect()
    
    private let appContext: AppActiveContextWatchOS
    
    @State private var appAlert: AppAlert?
    
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
                .frame(height: 53.0.scaled(appContext))
                .frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .buttonStyle(PlainButtonStyle())
                .background {
                    colorInt(0xFF1A1A1A)
                        .asColor()
                        .clipShape(RoundedRectangle(cornerRadius: 50))
                }
                Button(action: {
                    appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.fav))
                }) {
                    Image(systemName: "star.fill")
                        .font(.system(size: min(22.scaled(appContext, true), 24.scaled(appContext))))
                        .foregroundColor(.yellow)
                }
                .frame(width: 55.0.scaled(appContext), height: 53.0.scaled(appContext))
                .clipShape(RoundedRectangle(cornerRadius: 50))
                .buttonStyle(PlainButtonStyle())
                .background {
                    colorInt(0xFF1A1A1A)
                        .asColor()
                        .clipShape(RoundedRectangle(cornerRadius: 50))
                }
                if belowWidgetVersion {
                    Button(action: {
                        appContext.startActivity(appIntent: newAppIntent(appContext, AppScreen.etaTileList))
                        appContext.finishAffinity()
                    }) {
                        Image(systemName: "info.circle.fill")
                            .font(.system(size: min(21.scaled(appContext, true), 24.scaled(appContext))))
                            .foregroundColor(.red)
                    }
                    .frame(width: 55.0.scaled(appContext), height: 53.0.scaled(appContext))
                    .clipShape(RoundedRectangle(cornerRadius: 50))
                    .buttonStyle(PlainButtonStyle())
                    .background {
                        colorInt(0xFF1A1A1A)
                            .asColor()
                            .clipShape(RoundedRectangle(cornerRadius: 50))
                    }
                }
            }
            VStack {
                if let alert = appAlert {
                    if let url = alert.url {
                        Text(alert.content?.get(language: Shared().language) ?? "")
                            .lineLimit(2)
                            .lineSpacing(0)
                            .autoResizing(maxSize: 19.scaled(appContext, true), minSize: 11.scaled(appContext))
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFF6161).asColor())
                            .padding(.horizontal, 10.scaled(appContext))
                            .highPriorityGesture(
                                TapGesture()
                                    .onEnded { _ in
                                        appContext.handleWebpages(url: url, longClick: false, haptics: hapticsFeedback())()
                                    }
                            )
                    } else {
                        Text(alert.content?.get(language: Shared().language) ?? "")
                            .lineLimit(2)
                            .lineSpacing(0)
                            .autoResizing(maxSize: 19.scaled(appContext, true), minSize: 11.scaled(appContext))
                            .multilineTextAlignment(.center)
                            .foregroundColor(colorInt(0xFFFF6161).asColor())
                            .padding(.horizontal, 10.scaled(appContext))
                    }
                } else {
                    Text(Shared().language == "en" ? "HK Bus ETA" : "香港巴士到站預報")
                        .font(.system(size: 12.scaled(appContext)))
                    Text("v\(appContext.versionName) (\(appContext.versionCode.description)) @loohpjames")
                        .font(.system(size: 11.scaled(appContext))).padding(.bottom)
                }
            }
            .frame(height: 26.scaled(appContext, true))
            .padding(.bottom)
        }
        .padding()
        .onAppear {
            self.appAlert = AppContextWatchOSKt.getAppAlert(context: appContext)?.takeOrNull()
        }
        .onReceive(alertUpdateTimer) { _ in
            self.appAlert = AppContextWatchOSKt.getAppAlert(context: appContext)?.takeOrNull()
        }
    }
}
