package com.example.roomservice.data

import com.example.roomservice.data.model.NearbyPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NearbyPlacesRepository {
    private val _places = MutableStateFlow<List<NearbyPlace>>(
        listOf(
            NearbyPlace("1", "Kashi Chat Bhandar", "Banarasi Special", true, "300m", 300, 25.3090, 83.0076, 4.8, 1250, "Godowlia", phone="+919999999991", mapUrl="https://maps.google.com/?q=Kashi+Chat+Bhandar", swiggyUrl="https://swiggy.com", zomatoUrl="https://zomato.com"),
            NearbyPlace("2", "Baati Chokha", "Dhaba", true, "450m", 450, 25.3283, 82.9938, 4.5, 850, "Teliabagh", phone="+919999999992", mapUrl="https://maps.google.com/?q=Baati+Chokha", zomatoUrl="https://zomato.com"),
            NearbyPlace("3", "Keshav Paan", "Paan Shop", null, "150m", 150, 25.2818, 83.0006, 4.9, 2100, "Lanka", phone="+919999999993", mapUrl="https://maps.google.com/?q=Keshav+Paan"),
            NearbyPlace("4", "Shree Shivay", "Bhojanalay", true, "200m", 200, 25.3182, 82.9872, 4.2, 300, "Station Road", phone="+919999999994", mapUrl="https://maps.google.com/?q=Shree+Shivay"),
            NearbyPlace("5", "Dakshin", "South Indian", true, "400m", 400, 25.3095, 82.9875, 4.4, 500, "Sigra", phone="+919999999995", mapUrl="https://maps.google.com/?q=Dakshin", swiggyUrl="https://swiggy.com"),
            NearbyPlace("6", "Great Kebab", "Non-Veg", false, "500m", 500, 25.3340, 82.9810, 4.6, 750, "Mall Road", phone="+919999999996", mapUrl="https://maps.google.com/?q=Great+Kebab", zomatoUrl="https://zomato.com"),
            NearbyPlace("7", "Pizza Hut", "Fast Food", false, "600m", 600, 25.3200, 82.9850, 4.1, 1500, "IP Mall", phone="+919999999997", mapUrl="https://maps.google.com/?q=Pizza+Hut"),
            NearbyPlace("8", "Assi Hatti", "Hatti", true, "50m", 50, 25.2890, 83.0066, 4.7, 900, "Assi Ghat", phone="+919999999998", mapUrl="https://maps.google.com/?q=Assi+Hatti"),
            NearbyPlace("9", "Dosa Plaza", "South Indian", true, "750m", 750, 25.3080, 83.0060, 4.0, 400, "Godowlia", mapUrl="https://maps.google.com/?q=Dosa+Plaza"),
            NearbyPlace("10", "Ming Garden", "Fast Food", false, "900m", 900, 25.3320, 82.9750, 4.3, 600, "Cantonment", mapUrl="https://maps.google.com/?q=Ming+Garden"),
            // Wine Shops
            NearbyPlace("11", "English Wine Shop", "Wine Shop", null, "200m", 200, 25.3050, 83.0080, 4.2, 150, "Godowlia Chauraha", phone="+919999999911", mapUrl="https://maps.google.com/?q=English+Wine+Shop+Godowlia"),
            NearbyPlace("12", "Model Shop", "Wine Shop", null, "450m", 450, 25.3120, 83.0020, 4.0, 200, "Lanka Road", phone="+919999999912", mapUrl="https://maps.google.com/?q=Model+Shop+Varanasi"),
            NearbyPlace("13", "Premium Liquor Store", "Wine Shop", null, "800m", 800, 25.3200, 82.9950, 4.5, 310, "Cantonment", phone="+919999999913", mapUrl="https://maps.google.com/?q=Premium+Liquor+Store"),
            NearbyPlace("14", "Beer & Wine Shop", "Wine Shop", null, "1.2km", 1200, 25.3350, 82.9800, 3.9, 85, "Sigra Road", phone="+919999999914", mapUrl="https://maps.google.com/?q=Beer+Wine+Shop+Sigra")
        )
    )
    val places: StateFlow<List<NearbyPlace>> = _places.asStateFlow()
}