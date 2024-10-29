//
//  ColorUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import Foundation
import SwiftUI

extension UIColor {
    
    public convenience init?(hex: String) {
        let r, g, b, a: CGFloat

        let start = hex.index(hex.startIndex, offsetBy: 0)
        let hexColor = String(hex[start...])

        if hexColor.count == 8 {
            let scanner = Scanner(string: hexColor)
            var hexNumber: UInt64 = 0

            if scanner.scanHexInt64(&hexNumber) {
                a = CGFloat((hexNumber & 0xff000000 as UInt64) >> 24) / 255
                r = CGFloat((hexNumber & 0x00ff0000 as UInt64) >> 16) / 255
                g = CGFloat((hexNumber & 0x0000ff00 as UInt64) >> 8) / 255
                b = CGFloat(hexNumber & 0x000000ff as UInt64) / 255

                self.init(red: r, green: g, blue: b, alpha: a)
                return
            }
        }

        return nil
    }
    
    func adjustBrightness(percentage: CGFloat) -> UIColor {
        if percentage == 1 {
            return self
        }

        var hue: CGFloat = 0
        var saturation: CGFloat = 0
        var brightness: CGFloat = 0
        var alpha: CGFloat = 0

        getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: &alpha)

        if percentage > 1 {
            saturation *= (1 - (percentage - 1)).clamped(to: 0...1)
        } else {
            brightness *= percentage.clamped(to: 0...1)
        }

        return UIColor(hue: hue, saturation: saturation, brightness: brightness, alpha: alpha)
    }
    
    func withAlpha(alpha: Int) -> UIColor {
        return self.withAlphaComponent(CGFloat(alpha) / 255)
    }
    
    func toColor() -> Color {
        return Color(uiColor: self)
    }
    
}

extension Color {
    
    func adjustBrightness(percentage: CGFloat) -> Color {
        return UIColor(self).adjustBrightness(percentage: percentage).toColor()
    }
    
    func adjustAlpha(percentage: CGFloat) -> Color {
        return opacity(percentage)
    }
    
    func withAlpha(alpha: Int) -> Color {
        return UIColor(self).withAlpha(alpha: alpha).toColor()
    }
    
}

extension Int {
    
    func asUIColor() -> UIColor {
        return Int64(self).asUIColor()
    }
    
    func asColor() -> Color {
        return asUIColor().toColor()
    }
    
}

extension Int64 {
    
    func asUIColor() -> UIColor {
        let r, g, b, a: CGFloat
        a = CGFloat((self & 0xff000000 as Int64) >> 24) / 255
        r = CGFloat((self & 0x00ff0000 as Int64) >> 16) / 255
        g = CGFloat((self & 0x0000ff00 as Int64) >> 8) / 255
        b = CGFloat(self & 0x000000ff as Int64) / 255
        return UIColor(red: r, green: g, blue: b, alpha: a)
    }
    
    func asColor() -> Color {
        return asUIColor().toColor()
    }
    
}
