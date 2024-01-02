//
//  StringUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import Foundation
import WatchKit
import SwiftUI
import shared

extension FormattedText {
    
    func asAttributedString(defaultFontSize: CGFloat) -> AttributedString {
        let attributedString = NSMutableAttributedString(string: self.string)
        attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: defaultFontSize), range: NSRange(location: 0, length: attributedString.string.unicodeScalars.map { $0.value > 0xFFFF ? 2 : 1 }.reduce(0, +)))
        var index = 0
        for obj in self.content {
            let textContent = obj as! FormattedTextContent
            let length = textContent.string.unicodeScalars.map { $0.value > 0xFFFF ? 2 : 1 }.reduce(0, +)
            let range = NSMakeRange(index, length)
            index += length
            var sizeMultiplier = 1.0
            var bold = false
            for style in textContent.style {
                if style is SmallContentStyle {
                    sizeMultiplier = 0.8
                } else if style is BigContentStyle {
                    sizeMultiplier = 1.25
                } else if style is BoldContentStyle {
                    bold = true
                } else if style is ColorContentStyle {
                    let colorStyle = style as! ColorContentStyle
                    attributedString.addAttribute(.foregroundColor, value: colorStyle.color.asUIColor(), range: range)
                }
            }
            attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: defaultFontSize * sizeMultiplier, weight: bold ? .bold : .regular), range: range)
        }
        return AttributedString(attributedString)
    }
    
}

extension String {
    
    func asAttributedString(color: Color? = nil, fontSize: CGFloat? = nil) -> AttributedString {
        let attributedString = NSMutableAttributedString(string: self)
        let range = NSMakeRange(0, self.count)
        if color != nil {
            attributedString.addAttribute(.foregroundColor, value: UIColor(color!), range: range)
        }
        if fontSize != nil {
            attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: fontSize!), range: range)
        }
        return AttributedString(attributedString)
    }
    
    func eitherContains(other: String) -> Bool {
        return self.contains(other) || other.contains(self)
    }
    
}
