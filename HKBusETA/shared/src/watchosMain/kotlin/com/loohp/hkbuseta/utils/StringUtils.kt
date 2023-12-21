/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.utils

//import com.loohp.hkbuseta.common.utils.BigContentStyle
//import com.loohp.hkbuseta.common.utils.ColorContentStyle
//import com.loohp.hkbuseta.common.utils.FormattedText
//import com.loohp.hkbuseta.common.utils.InlineImageStyle
//import com.loohp.hkbuseta.common.utils.SmallContentStyle
//import kotlinx.cinterop.BetaInteropApi
//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.UnsafeNumber
//import platform.Foundation.NSAttributedString
//import platform.Foundation.NSMakeRange
//import platform.Foundation.NSMutableAttributedString
//import platform.Foundation.addAttribute
//import platform.Foundation.create
//import platform.UIKit.NSFontAttributeName
//import platform.UIKit.NSForegroundColorAttributeName
//import platform.UIKit.UIColor
//import platform.UIKit.UIFont
//import platform.UIKit.UIImage
//

//const val defaultFontSize: Float = 16F
//
//@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class, UnsafeNumber::class)
//fun FormattedText.asAttributedString(): NSAttributedString {
//    val attributedString = NSMutableAttributedString.create(string)
//    var index = 0
//    for (textContent in content) {
//        val range = NSMakeRange(index.toUInt(), textContent.string.length.toUInt())
//        index += textContent.string.length
//        for (style in textContent.style) {
//            when (style) {
//                is SmallContentStyle -> {
//                    attributedString.addAttribute(NSFontAttributeName, UIFont.systemFontOfSize(defaultFontSize * 0.8F), range)
//                }
//                is BigContentStyle -> {
//                    attributedString.addAttribute(NSFontAttributeName, UIFont.systemFontOfSize(defaultFontSize * 1.25F), range)
//                }
//                is ColorContentStyle -> {
//                    val argbValue = style.color.toULong()
//                    val alpha = ((argbValue shr 24) and 0xFFu).toFloat() / 255.0F
//                    val red = ((argbValue shr 16) and 0xFFu).toFloat() / 255.0F
//                    val green = ((argbValue shr 8) and 0xFFu).toFloat() / 255.0F
//                    val blue = (argbValue and 0xFFu).toFloat() / 255.0F
//                    attributedString.addAttribute(NSForegroundColorAttributeName, UIColor.colorWithRed(red, green = green, blue = blue, alpha = alpha), range)
//                }
//                is InlineImageStyle -> {
//                    val imageAttachment = TextAttachment().apply {
//                        // Assuming you have an UIImage instance
//                        // You can load an image from your resources or assets
//                        image = UIImage.imageNamed("yourImageName") // Replace with your image name
//                        bounds = CGRectMake(0.0, 0.0, 20.0, 20.0) // Set the image size
//                    }
//
//                    // Create an attributed string with the image
//                    val imageString = NSAttributedString.attributedStringWithAttachment(imageAttachment)
//
//                    // Append the image to the full string
//                    fullString.appendAttributedString(imageString)
//
//                    // Continue with the rest of the text
//                    fullString.appendString(" Apple")
//
//                    return fullString
//                }
//            }
//        }
//    }
//}