//
//  MathUtils.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 22/12/2023.
//

import Foundation

extension Comparable {
    
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
    
}
