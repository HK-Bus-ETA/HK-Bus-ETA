//
//  MathUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import Foundation
import SwiftUI

let kotlinMaxDouble: Double = 1.7976931348623157E308

extension Comparable {
    
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
    
}

func colorInt(_ value: Int64) -> Int {
    return Int(truncatingIfNeeded: value)
}

extension Int {
    
    func asInt32() -> Int32 {
        return Int32(clamping: self)
    }
    
    func dynamicSize() -> Int {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(Int((Double(self) * 1.2).rounded()), Int(UIFontMetrics.default.scaledValue(for: Double(self)).rounded()))
    }
    
}

extension Float {
    
    func dynamicSize() -> Float {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(self * 1.2, Float(UIFontMetrics.default.scaledValue(for: Double(self))))
    }
    
}

extension Double {
    
    func dynamicSize() -> Double {
        @Environment(\.sizeCategory) var sizeCategory
        return Swift.min(self * 1.2, UIFontMetrics.default.scaledValue(for: self))
    }
    
}

