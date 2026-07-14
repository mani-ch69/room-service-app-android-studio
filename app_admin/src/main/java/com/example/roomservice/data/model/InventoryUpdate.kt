package com.example.roomservice.data.model

data class InventoryUpdate(
    val date: Long = 0L,
    val roomType: String = "",
    val roomsToSell: Int = 1,
    val price: Double = 0.0,
    val isOpen: Boolean = true
)
