//
//  WidgetProcessUtils.swift
//  FavouriteRoutesWidgetExtensionExtension
//
//  Created by LOOHP on 31/10/2024.
//

import Foundation
import shared


func restrictKotlinHeap() {
    WidgetHelperKt.restrictHeapSize(bytes: 5242880)
}
