package com.example.roomservice.data.model

data class BusinessDetails(
    var hotelName: String = "My Hotel",
    var address: String = "",
    var gstNumber: String = "",
    var phone: String = "",
    var email: String = "",
    var logoUrl: String = "",
    var description: String = "",
    var propertyType: String = "Hotel",
    var starRating: String = "3",
    var checkInTime: String = "12:00 PM",
    var checkOutTime: String = "11:00 AM"
)
