//
//  ComposeView.swift
//  HKBusETA Phone App
//
//  Created by LOOHP on 31/01/2024.
//

import UIKit
import SwiftUI
import ComposeApp

struct ComposeUIView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) { /* do nothing */ }
}

struct ComposeView: View {

    @Environment(\.verticalSizeClass) var verticalSizeClass
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    var body: some View {
        GeometryReader { geometry in
            ComposeUIView()
                .ignoresSafeArea(.all)
                .onAppear {
                    AppContextCompose_iosKt.windowWidth = geometry.size.width
                    AppContextCompose_iosKt.windowHeight = geometry.size.height
                }
                .onChange(of: geometry.size) { newSize in
                    AppContextCompose_iosKt.windowWidth = newSize.width
                    AppContextCompose_iosKt.windowHeight = newSize.height
                }
        }
    }
}
