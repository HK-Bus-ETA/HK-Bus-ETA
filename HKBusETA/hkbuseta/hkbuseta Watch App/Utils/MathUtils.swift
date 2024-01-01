//
//  MathUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import Foundation

let kotlinMaxDouble: Double = 1.7976931348623157E308

extension Comparable {
    
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
    
}

func colorInt(_ value: Int64) -> Int {
    return Int(truncatingIfNeeded: value)
}
