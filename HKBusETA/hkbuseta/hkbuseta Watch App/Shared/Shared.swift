//
//  Shared.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import Foundation
import WatchKit
import SwiftUI
import shared

func appContext() -> AppActiveContextWatchOS {
    return AppContextWatchOSKt.appContext
}

func registry() -> Registry {
    return Registry.Companion().getInstance(context: appContext())
}

func playHaptics() {
    WKInterfaceDevice.current().play(.success)
}

func newAppDataConatiner() -> KotlinMutableDictionary<NSString, AnyObject> {
    return AppContextWatchOSKt.createMutableAppDataContainer()
}

func BackButton(predicate: @escaping (HistoryStackEntry) -> Bool) -> some View {
    ZStack {
        Button(action: {
            appContext().popStackIfMatches { predicate($0).asKt() }
        }) {
            Image(systemName: "arrow.left")
                .font(.system(size: 17))
                .bold()
                .foregroundColor(.white)
        }
        .frame(width: 30, height: 30)
        .buttonStyle(PlainButtonStyle())
        .position(x: 23, y: 23)
    }
    .frame(
        maxWidth: .infinity,
        maxHeight: .infinity,
        alignment: .top
    )
    .edgesIgnoringSafeArea(.all)
}

extension View {
    
    func CrossfadeText(textList: [AttributedString], state: Int) -> some View {
        Text(textList[state % textList.count])
            .id(textList[state % textList.count])
            .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    func CrossfadeMarqueeText(textList: [AttributedString], state: Int, font: UIFont, leftFade: CGFloat, rightFade: CGFloat, startDelay: Double, alignment: Alignment? = nil) -> some View {
        MarqueeText(
            text: textList[state % textList.count],
            font: font,
            leftFade: leftFade,
            rightFade: rightFade,
            startDelay: startDelay,
            alignment: alignment
        )
        .id(textList[state % textList.count])
        .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    func autoResizing(maxSize: CGFloat = 200) -> some View {
        self.font(.system(size: maxSize)).minimumScaleFactor(0.0001)
    }
    
}

extension Int {
    
    func asInt32() -> Int32 {
        return Int32(clamping: self)
    }
    
}

extension Bool {
    
    func asKt() -> KotlinBoolean {
        return KotlinBoolean(bool: self)
    }
    
}
