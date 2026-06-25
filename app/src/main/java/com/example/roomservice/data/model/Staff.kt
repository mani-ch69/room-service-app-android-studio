package com.example.roomservice.data.model

import java.util.UUID

data class Staff(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var code: String = "", // 4-6 digit login code
    var phone: String = "",
    var role: String = "WAITER", // WAITER, HOUSEKEEPING, etc.
    var isAvailable: Boolean = true,
    var hotelId: String = ""
)
