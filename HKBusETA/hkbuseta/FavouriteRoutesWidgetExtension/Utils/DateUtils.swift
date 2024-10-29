//
//  DateUtils.swift
//  FavouriteRoutesWidgetExtensionExtension
//
//  Created by LOOHP on 31/10/2024.
//

import Foundation
import shared


extension DateComponents {
    
    func toLocalDate() -> Date {
        defer { restrictKotlinHeap() }
        var dateComponent = self
        dateComponent.timeZone = TimeZone(identifier: TimeUtils_iosKt.hongKongZoneId)!
        return Calendar.current.date(from: dateComponent)!
    }
    
    func toFormattedDate() -> String {
        defer { restrictKotlinHeap() }
        return WidgetHelperKt.formatTimeWidget(self)
    }
    
}
