//
//  ShareExtensionView.swift
//  ShareExtension
//
//  Created by LOOHP on 21/01/2024.
//

import SwiftUI
import WatchConnectivity

struct ShareExtensionView: View {
    
    @State private var url: String
    
    init(url: String) {
        self.url = url
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 40) {
            VStack(alignment: .center) {
                Text("香港巴士到站預報 (WatchOS)")
                    .multilineTextAlignment(.center)
                    .font(.title.bold())
                Text("HK Bus ETA (WatchOS)")
                    .multilineTextAlignment(.center)
                    .font(.title.bold())
                Link(destination: URL(string: "https://loohpjames.com")!, label: {
                    Text("@LoohpJames")
                        .foregroundColor(.black)
                        .multilineTextAlignment(.center)
                        .font(.subheadline)
                })
            }
            .frame(maxWidth: .infinity)
            VStack(alignment: .leading) {
                Text("本手機配套應用程式用於直接讓你在手錶上開啟巴士路線連結。")
                    .multilineTextAlignment(.leading)
                    .font(.title3)
                Text("使用此功能時，手錶應用程式必須位於前台。")
                    .multilineTextAlignment(.leading)
                    .font(.title3)
                Text("This phone companion app is used for directly launching into bus routes and stops onto the watch app.")
                    .multilineTextAlignment(.leading)
                    .font(.title3)
                Text("The watch app must be in the foreground while this feature is used.")
                    .multilineTextAlignment(.leading)
                    .font(.title3)
            }
            .frame(maxWidth: .infinity)
            Link(destination: URL(string: "https://watch.hkbus.app/shared?url=\(url)")!, label: {
                Text("開啟路線 Launch Route")
                    .frame(maxWidth: .infinity)
            })
            .environment(\.openURL, OpenURLAction { url in
                close()
                return .systemAction(url)
            })
            Button(action: {
                close()
            }) {
                Text("關閉 Close")
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
        .padding()
    }
    
    func close() {
        NotificationCenter.default.post(name: NSNotification.Name("close"), object: nil)
    }
}
