package com.example.roomservice.data

import com.example.roomservice.data.model.MenuItem
import com.example.roomservice.data.model.Category
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShopRepository {
    private var db = FirebaseDatabase.getInstance().getReference("hotels")
    private val _shopItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val shopItems: StateFlow<List<MenuItem>> = _shopItems.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    fun startListening(hotelId: String) {
        val shopRef = db.child(hotelId).child("shop")
        shopRef.keepSynced(true)
        
        shopRef.child("items").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(MenuItem::class.java) }
                _shopItems.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        shopRef.child("categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Category::class.java) }
                _categories.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addItem(item: MenuItem) {
        val hotelId = HotelSession.hotelId.value ?: return
        item.itemType = "SHOP"
        db.child(hotelId).child("shop").child("items").child(item.id).setValue(item)
    }

    fun updateItem(item: MenuItem) {
        addItem(item)
    }

    fun deleteItem(itemId: String) {
        val hotelId = HotelSession.hotelId.value ?: return
        db.child(hotelId).child("shop").child("items").child(itemId).removeValue()
    }

    fun toggleAvailability(itemId: String, isAvailable: Boolean) {
        val hotelId = HotelSession.hotelId.value ?: return
        db.child(hotelId).child("shop").child("items").child(itemId).child("isAvailable").setValue(isAvailable)
    }

    fun updateStock(itemId: String, newStock: Int) {
        val hotelId = HotelSession.hotelId.value ?: return
        db.child(hotelId).child("shop").child("items").child(itemId).child("stock").setValue(newStock)
    }

    fun addCategory(name: String) {
        val hotelId = HotelSession.hotelId.value ?: return
        val id = java.util.UUID.randomUUID().toString()
        db.child(hotelId).child("shop").child("categories").child(id).setValue(Category(id, name))
    }

    fun deleteCategory(categoryId: String) {
        val hotelId = HotelSession.hotelId.value ?: return
        db.child(hotelId).child("shop").child("categories").child(categoryId).removeValue()
    }

    fun clearData() {
        _shopItems.value = emptyList()
        _categories.value = emptyList()
    }
}
