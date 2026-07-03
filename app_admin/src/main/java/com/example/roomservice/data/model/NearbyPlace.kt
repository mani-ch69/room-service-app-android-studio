package com.example.roomservice.data.model

data class NearbyPlace(
    val id: String,
    val name: String,
    val category: String, // Veg, Non-Veg, Fast Food, Paan Shop, Dhaba, Bhojanalay, Hatti, South Indian, Banarasi Special
    val isVeg: Boolean? = null,
    val distance: String, // Display distance (e.g., "300m")
    val distanceValue: Int, // distance in meters for initial mock sorting
    val latitude: Double, // Real latitude
    val longitude: Double, // Real longitude
    val rating: Double,
    val reviewCount: Int = 0,
    val address: String,
    val phone: String = "",
    val mapUrl: String = "",
    val swiggyUrl: String = "",
    val zomatoUrl: String = "",
    val imageUrl: String = ""
)