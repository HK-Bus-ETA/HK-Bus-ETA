//
//  MainView.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import SwiftUI
import shared
import WatchConnectivity

struct AlightReminderView: AppScreenView {
    
    @StateObject private var alightReminderService = StateFlowNullableObservable(stateFlow: AppContextWatchOSKt.remoteAlightReminderService, initSubscribe: true)
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    private let appContext: AppActiveContextWatchOS
    
    init(appContext: AppActiveContextWatchOS, data: [String: Any], storage: KotlinMutableDictionary<NSString, AnyObject>) {
        self.appContext = appContext
    }
    
    var body: some View {
        let service = alightReminderService.state
        VStack(alignment: .center, spacing: 2) {
            if service != nil {
                Text(service!.titleLeading)
                    .multilineTextAlignment(.center)
                    .autoResizing(maxSize: 19.scaled(appContext, true), minSize: 15.scaled(appContext, true), weight: .bold)
                    .fixedSize(horizontal: false, vertical: true)
                Text(service!.titleTrailing)
                    .multilineTextAlignment(.center)
                    .font(.system(size: 13.scaled(appContext, true)))
                    .fixedSize(horizontal: false, vertical: true)
                let textSize: CGFloat = 22.scaled(appContext, true)
                let text = {
                    if Shared().language == "en" {
                        "\(service!.stopsRemaining)".asAttributedString(fontSize: textSize * 1.25, weight: .bold) + " stops left".asAttributedString()
                    } else {
                        "剩餘 ".asAttributedString() + "\(service!.stopsRemaining)".asAttributedString(fontSize: textSize * 1.25, weight: .bold) + " 個站".asAttributedString()
                    }
                }()
                Text(text)
                    .multilineTextAlignment(.center)
                    .font(.system(size: textSize))
                    .fixedSize(horizontal: false, vertical: true)
                Text(service!.content)
                    .multilineTextAlignment(.center)
                    .font(.system(size: 13.scaled(appContext, true)))
                    .fixedSize(horizontal: false, vertical: true)
                Spacer().frame(fixedSize: 2)
                if !ambientMode {
                    Button(action: {
                        let payload = ["messageType": Shared().TERMINATE_ALIGHT_REMINDER_ID]
                        if WCSession.default.isReachable {
                            WCSession.default.sendMessage(payload) { _ in }
                        }
                    }) {
                        Image(systemName: "xmark").font(.system(size: 31.scaled(appContext, true))).foregroundColor(.red)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 50))
                    .frame(width: 55.scaled(appContext), height: 55.scaled(appContext))
                }
            }
        }.onAppear {
            if alightReminderService.state == nil {
                appContext.finish()
            }
        }.onChange(of: alightReminderService.state) { state in
            if state == nil {
                appContext.finish()
            }
        }
    }
}
