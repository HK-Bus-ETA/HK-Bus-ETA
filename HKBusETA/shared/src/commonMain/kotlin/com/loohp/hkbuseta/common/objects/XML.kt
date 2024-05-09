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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class MTRStatus(
    @XmlSerialName("lastBuildDate", prefix = "ryg_status") @XmlElement val lastBuildDate: String,
    @XmlSerialName("refreshInterval", prefix = "ryg_status") @XmlElement val refreshInterval: String,
    @XmlSerialName("line", prefix = "ryg_status") val lines: List<MTRLineStatus>,
)

@Serializable
data class MTRLineStatus(
    @XmlSerialName("line_code") @XmlElement val line: String,
    @XmlSerialName("url_tc") @XmlElement val urlZh: String,
    @XmlSerialName("url_en") @XmlElement val urlEn: String,
    @XmlSerialName("status") @XmlElement val status: String
)

@Serializable
data class TrafficNews(
    @XmlSerialName("message") val messages: List<TrafficNewsEntry>
)

@Serializable
data class TrafficNewsEntry(
    @XmlSerialName("INCIDENT_NUMBER") @XmlElement val incidentNumber: String,
    @XmlSerialName("INCIDENT_HEADING_EN") @XmlElement val incidentHeadingEN: String,
    @XmlSerialName("INCIDENT_HEADING_CN") @XmlElement val incidentHeadingCN: String,
    @XmlSerialName("INCIDENT_DETAIL_EN") @XmlElement val incidentDetailEN: String,
    @XmlSerialName("INCIDENT_DETAIL_CN") @XmlElement val incidentDetailCN: String,
    @XmlSerialName("LOCATION_EN") @XmlElement val locationEN: String?,
    @XmlSerialName("LOCATION_CN") @XmlElement val locationCN: String?,
    @XmlSerialName("DISTRICT_EN") @XmlElement val districtEN: String?,
    @XmlSerialName("DISTRICT_CN") @XmlElement val districtCN: String?,
    @XmlSerialName("DIRECTION_EN") @XmlElement val directionEN: String?,
    @XmlSerialName("DIRECTION_CN") @XmlElement val directionCN: String?,
    @XmlSerialName("ANNOUNCEMENT_DATE") @XmlElement val announcementDate: String,
    @XmlSerialName("INCIDENT_STATUS_EN") @XmlElement val incidentStatusEN: String,
    @XmlSerialName("INCIDENT_STATUS_CN") @XmlElement val incidentStatusCN: String,
    @XmlSerialName("NEAR_LANDMARK_EN") @XmlElement val nearLandmarkEN: String?,
    @XmlSerialName("NEAR_LANDMARK_CN") @XmlElement val nearLandmarkCN: String?,
    @XmlSerialName("BETWEEN_LANDMARK_EN") @XmlElement val betweenLandmarkEN: String?,
    @XmlSerialName("BETWEEN_LANDMARK_CN") @XmlElement val betweenLandmarkCN: String?,
    @XmlSerialName("ID") @XmlElement val id: String,
    @XmlSerialName("CONTENT_EN") @XmlElement val contentEN: String,
    @XmlSerialName("CONTENT_CN") @XmlElement val contentCN: String,
    @XmlSerialName("LATITUDE") @XmlElement val latitude: String?,
    @XmlSerialName("LONGITUDE") @XmlElement val longitude: String?
)

@Serializable
data class SpecialTrafficNews(
    @XmlSerialName("message") val messages: List<SpecialTrafficNewsEntry>
)

@Serializable
data class SpecialTrafficNewsEntry(
    @XmlSerialName("msgID") @XmlElement val msgID: String,
    @XmlSerialName("CurrentStatus") @XmlElement val currentStatus: String,
    @XmlSerialName("ChinText") @XmlElement val chinText: String,
    @XmlSerialName("ChinShort") @XmlElement val chinShort: String,
    @XmlSerialName("EngText") @XmlElement val engText: String,
    @XmlSerialName("EngShort") @XmlElement val engShort: String,
    @XmlSerialName("ReferenceDate") @XmlElement val referenceDate: String,
    @XmlSerialName("IncidentRefNo") @XmlElement val incidentRefNo: String,
    @XmlSerialName("CountofDistricts") @XmlElement val countOfDistricts: String,
    @XmlSerialName("ListOfDistrict") val listOfDistrict: ListOfDistrict
)

@Serializable
data class ListOfDistrict(
    @XmlSerialName("District") val districts: List<String>
)