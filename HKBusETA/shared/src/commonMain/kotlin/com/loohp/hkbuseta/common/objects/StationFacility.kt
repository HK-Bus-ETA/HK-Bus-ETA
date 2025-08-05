package com.loohp.hkbuseta.common.objects

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


object StationFacilitySerializer : KSerializer<StationFacility> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StationFacility", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StationFacility {
        return StationFacility.fromDisplayName(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: StationFacility) {
        encoder.encodeString(value.displayName.en)
    }
}

@Serializable(with = StationFacilitySerializer::class)
@ConsistentCopyVisibility
data class StationFacility private constructor(
    val displayName: BilingualText,
    val icon: String?,
    private val excludeIfProvider: () -> Set<StationFacility> = { emptySet() }
): Comparable<StationFacility> {

    @Suppress("unused")
    companion object {

        private val entries = mutableListOf<StationFacility>()

        val CUSTOMER_SERVICE_CENTRE = register(StationFacility(
            displayName = "客務中心" withEn "Customer Service Centre",
            icon = "customer_service_centre.png"
        ))
        val TICKETS = register(StationFacility(
            displayName = "車票" withEn "Tickets",
            icon = "tickets.png"
        ))
        val LIFT = register(StationFacility(
            displayName = "升降機" withEn "Lift",
            icon = "lift.png"
        ))
        val TOILETS = register(StationFacility(
            displayName = "洗手間" withEn "Toilets",
            icon = "toilets.png"
        ))
        val BABYCARE_ROOM = register(StationFacility(
            displayName = "育嬰間" withEn "Babycare Room",
            icon = "babycare_room.png"
        ))
        val DRINKING_WATER = register(StationFacility(
            displayName = "飲用水" withEn "Drinking Water",
            icon = "drinking_water.png"
        ))
        val POST_BOX = register(StationFacility(
            displayName = "郵箱" withEn "Post Box",
            icon = "post_box.png"
        ))
        val AUTOMATED_EXTERNAL_DEFIBRILLATOR = register(StationFacility(
            displayName = "自動體外心臟除顫器" withEn "Automated External Defibrillator",
            icon = "aed.png"
        ))
        val VENDING_MACHINE = register(StationFacility(
            displayName = "自動售賣機" withEn "Vending Machine",
            icon = null
        ))
        val PHOTO_BOOTH = register(StationFacility(
            displayName = "照相機" withEn "Photo Booth",
            icon = null
        ))
        val BOOK_DROP = register(StationFacility(
            displayName = "還書箱" withEn "Book Drop",
            icon = "book_drop.png"
        ))
        val INFORMATION_COUNTER = register(StationFacility(
            displayName = "資訊台" withEn "Information Counter",
            icon = "information_counter.png"
        ))
        val TOURIST_SERVICES = register(StationFacility(
            displayName = "旅客服務" withEn "Tourist Services",
            icon = "tourist_services.png"
        ))
        val LEFT_BAGGAGE = register(StationFacility(
            displayName = "行李寄存" withEn "Left Baggage",
            icon = "left_baggage.png"
        ))
        val HANG_SENG_BANK_ATM = register(StationFacility(
            displayName = "恒生銀行自動櫃員機" withEn "Hang Seng Bank ATM",
            icon = null,
            excludeIfProvider = { setOf(HANG_SENG_BANK) }
        ))
        val BANK_OF_CHINA_ATM = register(StationFacility(
            displayName = "中國銀行自動櫃員機" withEn "Bank of China ATM",
            icon = null,
            excludeIfProvider = { setOf(BANK_OF_CHINA) }
        ))
        val ICBC_ATM = register(StationFacility(
            displayName = "工銀亞洲櫃員機" withEn "ICBC ATM",
            icon = null
        ))
        val CITIBANK_ATM = register(StationFacility(
            displayName = "花旗銀行自動櫃員機" withEn "Citibank ATM",
            icon = null
        ))
        val AEON_ATM = register(StationFacility(
            displayName = "Aeon自動櫃員機" withEn "Aeon ATM",
            icon = null
        ))
        val HONG_KONG_BANK_ATM = register(StationFacility(
            displayName = "滙豐銀行櫃員機" withEn "Hong Kong Bank ATM",
            icon = null,
            excludeIfProvider = { setOf(HONG_KONG_BANK) }
        ))
        val BANK_OF_EAST_ASIA_ATM = register(StationFacility(
            displayName = "東亞銀行自動櫃員機" withEn "The Bank of East Asia ATM",
            icon = null
        ))
        val HANG_SENG_BANK = register(StationFacility(
            displayName = "恒生銀行" withEn "Hang Seng Bank",
            icon = "hang_seng_bank.png"
        ))
        val BANK_OF_CHINA = register(StationFacility(
            displayName = "中國銀行" withEn "Bank of China",
            icon = null
        ))
        val HONG_KONG_BANK = register(StationFacility(
            displayName = "匯豐銀行" withEn "Hong Kong Bank",
            icon = null
        ))
        val TAXI_STAND = register(StationFacility(
            displayName = "的士站" withEn "Taxi Stand",
            icon = "taxi_stand.png"
        ))
        val CAR_PARK_SHROFF = register(StationFacility(
            displayName = "停車場繳費處" withEn "Car Park Shroff",
            icon = "car_park_shroff.png"
        ))
        val BOOKED_VEHICLES = register(StationFacility(
            displayName = "預約車輛" withEn "Booked Vehicles",
            icon = "booked_vehicles.png"
        ))
        val HOTEL_SHUTTLE = register(StationFacility(
            displayName = "酒店專車" withEn "Hotel Shuttle",
            icon = "hotel_shuttle.png"
        ))
        val URBAN_BUS_AND_TOUR_HOTEL_COACH_STATION = register(StationFacility(
            displayName = "市區巴士及團體/酒店巴士站" withEn "Urban Bus and Tour/Hotel Coach station",
            icon = "urban_bus_tour_hotel_station.png"
        ))
        val CROSS_HARBOUR_BUS_STOP = register(StationFacility(
            displayName = "過海巴士站" withEn "Cross-harbour Bus stop",
            icon = "cross_harbour_bus_stop.png"
        ))
        val SOCKET_FOR_POWERED_WHEELCHAIR = register(StationFacility(
            displayName = "電動輪椅專用充電插座" withEn "Socket for Powered Wheelchair",
            icon = "socket_powered_wheelchair.png"
        ))
        val WHEELCHAIR_VERTICAL_LIFTING_PLATFORM = register(StationFacility(
            displayName = "輪椅垂直升降台" withEn "Wheelchair Vertical Lifting Platform",
            icon = "wheelchair_lifting_platform.png"
        ))
        val ACCESSIBLE_TOILET = register(StationFacility(
            displayName = "無障礙洗手間" withEn "Accessible Toilet",
            icon = "accessible_toilet.png",
            excludeIfProvider = { setOf(TOILETS) }
        ))
        val MOBILE_CHARGING_FACILITY = register(StationFacility(
            displayName = "流動裝置充電設施" withEn "Mobile Charging Facility",
            icon = "mobile_charging_facility.png"
        ))
        val POLICE_POST = register(StationFacility(
            displayName = "警崗" withEn "Police Post",
            icon = "police_post.png"
        ))
        val POLICE_REPORTING_CENTRE = register(StationFacility(
            displayName = "警察報案中心" withEn "Police Reporting Centre",
            icon = "police_reporting_centre.png"
        ))
        val STATION_CAR_PARK = register(StationFacility(
            displayName = "車站停車場" withEn "Station Car Park",
            icon = "station_car_park.png"
        ))
        val MTR_GALLERY = register(StationFacility(
            displayName = "港鐵展廊" withEn "MTR Gallery",
            icon = null
        ))
        val SELF_SERVICE_POINT = register(StationFacility(
            displayName = "自助客務機" withEn "Self-Service Point",
            icon = "self_service_point.png"
        ))
        val LOST_PROPERTY_OFFICE = register(StationFacility(
            displayName = "失物辦事處" withEn "Lost Property Office",
            icon = "lost_property_office.png"
        ))
        val MTR_E_STORE = register(StationFacility(
            displayName = "MTR e-Store".asBilingualText(),
            icon = "mtr_e_store.png"
        ))
        
        private fun register(facility: StationFacility): StationFacility {
            entries.add(facility)
            return facility
        }
        
        fun fromDisplayName(englishName: String): StationFacility {
            return entries.find { it.displayName.en == englishName }
                ?: register(StationFacility(
                    displayName = englishName.asBilingualText(),
                    icon = null
                ))
        }
    }

    val excludeIf by lazy { excludeIfProvider.invoke() }
    val ordinal by lazy { entries.indexOf(this) }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun compareTo(other: StationFacility): Int {
        return ordinal - other.ordinal
    }

}