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

func encoding(from charsetName: String) -> String.Encoding {
    switch charsetName {
    case "UTF-8", "utf-8":
        return .utf8
    case "US-ASCII", "us-ascii":
        return .ascii
    case "ISO-8859-1", "iso-8859-1":
        return .isoLatin1
    case "ISO-8859-2", "iso-8859-2":
        return .isoLatin2
    case "WINDOWS-1251", "windows-1251":
        return .windowsCP1251
    case "WINDOWS-1252", "windows-1252":
        return .windowsCP1252
    case "WINDOWS-1253", "windows-1253":
        return .windowsCP1253
    case "WINDOWS-1254", "windows-1254":
        return .windowsCP1254
    default:
        print("Warning: Unknown charset \(charsetName). Defaulting to UTF-8.")
        return .utf8
    }
}

extension FormattedText {
    
    func asAttributedString(defaultFontSize: CGFloat, defaultWeight: UIFont.Weight = .regular) -> AttributedString {
        let attributedString = NSMutableAttributedString(string: self.string)
        attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: defaultFontSize, weight: defaultWeight), range: NSRange(location: 0, length: attributedString.string.unicodeScalars.map { $0.value > 0xFFFF ? 2 : 1 }.reduce(0, +)))
        var index = 0
        for obj in self.content {
            let textContent = obj
            let length = textContent.string.unicodeScalars.map { $0.value > 0xFFFF ? 2 : 1 }.reduce(0, +)
            let range = NSMakeRange(index, length)
            index += length
            var sizeMultiplier = 1.0
            var bold = defaultWeight == .bold
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

extension NSMutableAttributedString {
    
    func addAttributeBehind(_ name: NSAttributedString.Key, value: Any, range: NSRange) {
        var existingAttributes: [(attributes: [NSAttributedString.Key: Any], range: NSRange)] = []
        self.enumerateAttributes(in: NSRange(location: 0, length: self.length), options: []) { attributes, range, _ in
            existingAttributes.append((attributes, range))
        }
        self.addAttribute(name, value: value, range: range)
        for (attributes, range) in existingAttributes {
            self.addAttributes(attributes, range: range)
        }
    }
    
}
