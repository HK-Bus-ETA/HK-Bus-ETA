//
//  FatalErrorView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 18/01/2024.
//

import SwiftUI
import shared

struct FatalErrorView: AppScreenView {
    
    @State
    private var zh: String?
    @State
    private var en: String?
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
        self.zh = data["zh"] as? String
        self.en = data["en"] as? String
    }
    
    var body: some View {
        VStack(alignment: .center) {
            Text(zh ?? "發生錯誤")
                .multilineTextAlignment(.center)
                .font(.system(size: 16.scaled(appContext, true), weight: .bold))
            Text(en ?? "Fatal Error Occurred")
                .multilineTextAlignment(.center)
                .font(.system(size: 16.scaled(appContext, true), weight: .bold))
            if zh == nil && en == nil {
                Spacer().frame(fixedSize: 2.scaled(appContext))
                Text("巴士路線資料可能不完整\n點選重新載入刷新")
                    .multilineTextAlignment(.center)
                    .font(.system(size: 12.scaled(appContext, true)))
                Text("Bus route data might be corrupted\nClick reload to refresh")
                    .multilineTextAlignment(.center)
                    .font(.system(size: 11.scaled(appContext, true)))
            }
            Spacer().frame(fixedSize: 10.scaled(appContext))
            Button(action: {
                Shared().invalidateCache(context: appContext)
                let intent = newAppIntent(appContext, AppScreen.main)
                intent.addFlags(flags: [AppIntentFlag.theNewTask, AppIntentFlag.clearTask].asKt())
                appContext.startActivity(appIntent: intent)
                appContext.finishAffinity()
            }) {
                Image(systemName: "arrow.triangle.2.circlepath").font(.system(size: 21.scaled(appContext, true))).foregroundColor(.yellow)
            }
            .clipShape(RoundedRectangle(cornerRadius: 50))
            .frame(width: 155.scaled(appContext), height: 55.scaled(appContext))
        }
        .onAppear {
            registryClear()
        }
    }
}
