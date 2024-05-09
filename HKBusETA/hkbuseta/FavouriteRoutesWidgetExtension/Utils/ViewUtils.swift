//
//  ViewUtils.swift
//  HKBusETA Phone App
//
//  Created by LOOHP on 18/4/2024.
//

import Foundation
import SwiftUI

extension View {
    
    @ViewBuilder func autoResizing(maxSize: CGFloat = 200, minSize: CGFloat = 1, weight: Font.Weight = .regular) -> some View {
        self.font(.system(size: maxSize, weight: weight)).minimumScaleFactor(minSize / maxSize)
    }
    
}
