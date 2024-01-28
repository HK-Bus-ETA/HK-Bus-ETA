//
//  MarqueeText.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import SwiftUI

struct MarqueeText : View {
    
    @Environment(\.isLuminanceReduced) var ambientMode
    
    var oas: OptionalAttributedString
    var font: UIFont
    var startDelay: Double
    var alignment: Alignment
    
    @State private var animate = false
    var isCompact = false
    
    init(text: String, font: UIFont, startDelay: Double, alignment: Alignment? = nil) {
        self.oas = OptionalAttributedString(string: text)
        self.font = font
        self.startDelay = startDelay
        self.alignment = alignment != nil ? alignment! : .topLeading
    }
    
    init(text: AttributedString, font: UIFont, startDelay: Double, alignment: Alignment? = nil) {
        self.oas = OptionalAttributedString(attributedString: text, string: String(text.characters[...]))
        self.font = font
        self.startDelay = startDelay
        self.alignment = alignment != nil ? alignment! : .topLeading
    }
    
    func text() -> Text {
        return oas.attributedString == nil ? Text(oas.string) : Text(oas.attributedString!)
    }
    
    var body : some View {
        let stringWidth = oas.attributedString == nil ? oas.string.widthOfString(usingFont: font) : oas.attributedString!.widthOfString(usingFont: font)
        let stringHeight = oas.attributedString == nil ? oas.string.heightOfString(usingFont: font) : oas.attributedString!.heightOfString(usingFont: font)
        
        let animation = Animation
            .linear(duration: Double(stringWidth) / 30)
            .delay(startDelay)
            .repeatForever(autoreverses: false)
        
        let nullAnimation = Animation
            .linear(duration: 0)
        
        return ZStack {
            GeometryReader { geo in
                Group {
                    if stringWidth > geo.size.width {
                        Group {
                            text()
                                .lineLimit(1)
                                .font(.init(font))
                                .offset(x: self.animate ? -stringWidth - stringHeight * 2 : 0)
                                .animation(self.animate ? animation : nullAnimation, value: self.animate)
                                .onAppear {
                                    self.animate = geo.size.width < stringWidth && !ambientMode
                                }
                                .fixedSize(horizontal: true, vertical: false)
                                .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity, alignment: .topLeading)
                            text()
                                .lineLimit(1)
                                .font(.init(font))
                                .offset(x: self.animate ? 0 : stringWidth + stringHeight * 2)
                                .animation(self.animate ? animation : nullAnimation, value: self.animate)
                                .onAppear {
                                    self.animate = geo.size.width < stringWidth && !ambientMode
                                }
                                .fixedSize(horizontal: true, vertical: false)
                                .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity, alignment: .topLeading)
                        }
                        .mask(
                            HStack(spacing:0) {
                                Rectangle()
                                    .frame(width:0)
                                    .opacity(0)
                                LinearGradient(gradient: Gradient(colors: [Color.black, Color.black]), startPoint: .leading, endPoint: .trailing)
                                Rectangle()
                                    .frame(width:0)
                                    .opacity(0)
                            })
                        .frame(width: geo.size.width)
                    } else {
                        text()
                            .font(.init(font))
                            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity, alignment: alignment)
                    }
                }
                .onChange(of: oas) { oas in
                    self.animate = false
                    let stringWidth = oas.attributedString == nil ? oas.string.widthOfString(usingFont: font) : oas.attributedString!.widthOfString(usingFont: font)
                    self.animate = geo.size.width < stringWidth && !ambientMode
                }
                .onChange(of: ambientMode) { ambientMode in
                    self.animate = geo.size.width < stringWidth && !ambientMode
                }
            }
        }
        .frame(height: stringHeight)
        .frame(maxWidth: isCompact ? stringWidth : nil)
        .onDisappear { self.animate = false }
    }
}

struct OptionalAttributedString : Equatable {
    
    var attributedString: AttributedString?
    var string: String
    
}

extension MarqueeText {
    
    public func makeCompact(_ compact: Bool = true) -> Self {
        var view = self
        view.isCompact = compact
        return view
    }
    
}

extension String {
    
    func widthOfString(usingFont font: UIFont) -> CGFloat {
        let fontAttributes = [NSAttributedString.Key.font: font]
        let size = self.size(withAttributes: fontAttributes)
        return size.width
    }

    func heightOfString(usingFont font: UIFont) -> CGFloat {
        let fontAttributes = [NSAttributedString.Key.font: font]
        let size = self.size(withAttributes: fontAttributes)
        return size.height
    }
}

extension AttributedString {
    
    func widthOfString(usingFont font: UIFont) -> CGFloat {
        let nsa = NSMutableAttributedString(self)
        nsa.addAttributeBehind(.font, value: font, range: NSRange(location: 0, length: nsa.length))
        let size = nsa.size()
        return size.width
    }

    func heightOfString(usingFont font: UIFont) -> CGFloat {
        let nsa = NSMutableAttributedString(self)
        nsa.addAttributeBehind(.font, value: font, range: NSRange(location: 0, length: nsa.length))
        let size = nsa.size()
        return size.height
    }
}
