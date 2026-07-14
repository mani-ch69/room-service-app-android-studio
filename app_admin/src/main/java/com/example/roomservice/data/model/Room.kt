package com.example.roomservice.data.model

import java.util.UUID

data class Room(
    var roomNumber: String = "", // Used as Category Name/ID now
    var totalUnits: Int = 1,     // NEW: Number of rooms of this type
    var hotelId: String = "HOTEL_ID_01",
    var qrToken: String = UUID.randomUUID().toString(),
    var roomType: String = "Deluxe Room",
    var smokingPolicy: String = "Non-smoking",
    var floorLevel: String = "Ground floor",
    var bedType: String = "King Size",
    var numberOfBeds: Int = 1,
    var maxGuests: Int = 2,
    var maxAdults: Int = 2,
    var maxChildren: Int = 0,
    var numBathrooms: Int = 1,
    var isBathroomPrivate: Boolean = true,
    var roomSize: String = "250 sqft",
    var hasAc: Boolean = true,
    var isBathroomInside: Boolean = true,
    var hasGeyser: Boolean = true,
    var hasKettle: Boolean = true,
    var imageUrl: String = "",
    var isAvailable: Boolean = true
) {
    fun generateAppSchemeLink(): String {
        return "roomservice://scan?room=$roomNumber&hotel=$hotelId"
    }
}
