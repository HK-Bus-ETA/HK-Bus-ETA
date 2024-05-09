/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.hkbuseta.common.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FareType(
    val category: FareCategory,
    val ticketCategory: TicketCategory
) {

    @SerialName("octo_adult")
    OCTO_ADULT(FareCategory.ADULT, TicketCategory.OCTO),
    @SerialName("octo_child")
    OCTO_CHILD(FareCategory.CHILD, TicketCategory.OCTO),
    @SerialName("octo_elderly")
    OCTO_ELDERLY(FareCategory.ELDERLY, TicketCategory.OCTO),
    @SerialName("octo_joyyou_sixty")
    OCTO_JOYYOU_SIXTY(FareCategory.JOYYOU_SIXTY, TicketCategory.OCTO),
    @SerialName("octo_pwd")
    OCTO_PWD(FareCategory.PWD, TicketCategory.OCTO),
    @SerialName("octo_student")
    OCTO_STUDENT(FareCategory.STUDENT, TicketCategory.OCTO),
    @SerialName("single_adult")
    SINGLE_ADULT(FareCategory.ADULT, TicketCategory.SINGLE),
    @SerialName("single_child")
    SINGLE_CHILD(FareCategory.CHILD, TicketCategory.SINGLE),
    @SerialName("single_elderly")
    SINGLE_ELDERLY(FareCategory.ELDERLY, TicketCategory.SINGLE);

}

enum class FareCategory(
    val displayName: BilingualText
) {

    ADULT("成人" withEn "Adult"),
    CHILD("小童" withEn "Child"),
    ELDERLY("長者/樂悠卡(65歲或以上)" withEn "Elderly/JoyYou Card (Aged 65 or above)"),
    JOYYOU_SIXTY("樂悠卡(60至64歲)" withEn "JoyYou Card (Aged 60 - 64)"),
    PWD("殘疾人士" withEn "Persons with Disabilities"),
    STUDENT("學生" withEn "Student")

}

enum class TicketCategory(
    val displayName: BilingualText
) {

    OCTO("八達通" withEn "Octopus"),
    SINGLE("單程票" withEn "Single Ride Ticket")

}