//
//  ViewUtils.swift
//  HKBusETA Watch App
//
//  Created by LOOHP on 16/4/2024.
//

import Foundation
import SwiftUI
import shared

struct IndeterminateCircularProgressIndicator: View {
    
    let tintColor: Color
    let trackColor: Color
    let lineWidth: CGFloat
    
    @State private var tickCounter = 0.0

    var body: some View {
        ZStack {
            Circle().stroke(trackColor, lineWidth: lineWidth)
            Circle()
                .trim(from: tickCounter / 1000.0 - 1.0, to: tickCounter / 1000.0)
                .rotation(.degrees(tickCounter / 2000.0 * 720))
                .stroke(tintColor, lineWidth: lineWidth)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: false)) {
                tickCounter = 2000.0
            }
        }
    }
    
}

extension View {
    
    @ViewBuilder func apply<Content: View>(@ViewBuilder _ apply: @escaping (Self) -> Content) -> some View {
        apply(self)
    }
    
    @ViewBuilder func onSizeChange(_ perform: @escaping (CGSize) -> Void) -> some View {
        self.background {
            GeometryReader { geo in
                Color.clear
                    .onAppear { perform(geo.size) }
                    .onChange(of: geo.size) { size in perform(size) }
            }
        }
    }
    
    @ViewBuilder func tileStateBorder(_ state: TileUseState, _ cornerRadius: CGFloat) -> some View {
        switch state {
        case .primary:
            self.overlay {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(colorInt(0x5437FF00).asColor(), lineWidth: 2)
                    .padding(1)
            }
        case .secondary:
            self.overlay {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(colorInt(0x54FFB700).asColor(), lineWidth: 2)
                    .padding(1)
            }
        default:
            self
        }
    }
    
    @ViewBuilder func UserMarqueeText(text: String, font: UIFont, marqueeStartDelay: Double, marqueeAlignment: Alignment? = nil) -> some View {
        if Shared().disableMarquee {
            Text(text)
                .font(Font(font))
                .fixedSize(horizontal: false, vertical: true)
        } else {
            MarqueeText(
                text: text,
                font: font,
                startDelay: 2,
                alignment: marqueeAlignment
            )
            .lineLimit(1)
        }
    }
    
    @ViewBuilder func UserMarqueeText(text: AttributedString, font: UIFont, marqueeStartDelay: Double, marqueeAlignment: Alignment? = nil) -> some View {
        if Shared().disableMarquee {
            Text(text)
                .font(Font(font))
                .fixedSize(horizontal: false, vertical: true)
        } else {
            MarqueeText(
                text: text,
                font: font,
                startDelay: 2,
                alignment: marqueeAlignment
            )
            .lineLimit(1)
        }
    }
    
    @ViewBuilder func CrossfadeUserMarqueeText(textList: [AttributedString], state: Int, font: UIFont, marqueeStartDelay: Double, marqueeAlignment: Alignment? = nil) -> some View {
        if Shared().disableMarquee {
            CrossfadeText(
                textList: textList,
                state: state
            )
            .font(Font(font))
            .fixedSize(horizontal: false, vertical: true)
        } else {
            CrossfadeMarqueeText(
                textList: textList,
                state: state,
                font: font,
                startDelay: 2,
                alignment: marqueeAlignment
            )
            .lineLimit(1)
        }
    }
    
    @ViewBuilder func CrossfadeText(textList: [AttributedString], state: Int) -> some View {
        Text(textList[state % textList.count])
            .id(textList[state % textList.count])
            .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    @ViewBuilder func CrossfadeMarqueeText(textList: [AttributedString], state: Int, font: UIFont, startDelay: Double, alignment: Alignment? = nil) -> some View {
        MarqueeText(
            text: textList[state % textList.count],
            font: font,
            startDelay: startDelay,
            alignment: alignment
        )
        .id(textList[state % textList.count])
        .transition(.opacity.animation(.linear(duration: 0.5)))
    }
    
    @ViewBuilder func autoResizing(maxSize: CGFloat = 200, minSize: CGFloat = 1, weight: Font.Weight = .regular) -> some View {
        self.font(.system(size: maxSize, weight: weight)).minimumScaleFactor(minSize / maxSize)
    }
    
    @ViewBuilder func frame(fixedSize: CGFloat) -> some View {
        self.frame(width: fixedSize, height: fixedSize)
    }
    
}
