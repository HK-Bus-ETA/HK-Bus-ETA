//
//  ArrayUtils.swift
//  HKBusETA Watch App
//
//  Created by LOOHP on 16/4/2024.
//

import Foundation
import shared

extension Array where Element: NSObject {
    
    func asKt() -> KotlinArray<Element> {
        return KotlinArray(size: count.asInt32()) { index in self[Int(truncating: index)] }
    }
    
    func withIndex() -> [(Int, Element)] {
        return self.indices.map { ($0, self[$0]) }
    }
    
}
