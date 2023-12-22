//
//  Collector.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 21/12/2023.
//

import Foundation
import shared

class Collector<T> : Kotlinx_coroutines_coreFlowCollector {
    let block:(T) -> Void
    init(block: @escaping (T) -> Void) {
        self.block = block
    }

    func emit(value: Any?) async throws {
        if let parsed = value as? T {
            DispatchQueue.main.async {
                self.block(parsed)
            }
        }
    }
}
