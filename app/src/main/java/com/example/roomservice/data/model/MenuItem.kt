package com.example.roomservice.data.model

import java.util.UUID

enum class SelectionType {
    QUANTITY,
    SINGLE_REQUEST
}

data class MenuItem(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var price: Double = 0.0,
    var unit: String = "plate", // liter, kg, pc, plate
    var category: String = "",
    var description: String = "",
    var isAvailable: Boolean = true,
    var selectionType: SelectionType = SelectionType.QUANTITY,
    var freeLimit: Int = 0,
    var priceAfterLimit: Double = 0.0,
    var imageUrl: String = "",
    var stock: Int = -1, // -1 means infinite/menu item, >= 0 means inventory item
    var itemType: String = "RESTAURANT" // RESTAURANT or SHOP
)