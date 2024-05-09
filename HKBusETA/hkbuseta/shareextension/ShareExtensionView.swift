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
                Text("香港巴士到站預報")
                    .multilineTextAlignment(.center)
                    .font(.title.bold())
                Text("HK Bus ETA")
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
            Link(destination: URL(string: "https://app.hkbuseta.com/shared?url=\(url)")!, label: {
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
