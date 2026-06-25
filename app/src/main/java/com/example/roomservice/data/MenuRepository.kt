package com.example.roomservice.data

import com.example.roomservice.data.model.Category
import com.example.roomservice.data.model.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

object MenuRepository {
    private var db = FirebaseDatabase.getInstance().getReference("menu")
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private var catListener: ValueEventListener? = null
    private var itemListener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("menu")
        
        catListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Category::class.java) }
                _categories.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("categories").addValueEventListener(catListener!!)

        itemListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(MenuItem::class.java) }
                _menuItems.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("items").addValueEventListener(itemListener!!)
    }

    private fun stopListening() {
        catListener?.let { db.child("categories").removeEventListener(it) }
        itemListener?.let { db.child("items").removeEventListener(it) }
        catListener = null
        itemListener = null
    }

    private fun seedDefaultCategories() {
        val defaultCats = listOf("Main Course", "Drink", "Coffee", "Dessert", "Bakery")
        defaultCats.forEach { addCategory(it) }
    }

    fun addCategory(name: String) {
        val newCat = Category(name = name.trim())
        db.child("categories").child(newCat.id).setValue(newCat)
    }

    fun updateCategory(categoryId: String, newName: String) {
        val oldCategory = _categories.value.find { it.id == categoryId }
        val updatedName = newName.trim()
        
        db.child("categories").child(categoryId).child("name").setValue(updatedName)
        
        // Also update all items that were in this category
        if (oldCategory != null) {
            _menuItems.value.forEach { item ->
                if (item.category == oldCategory.name) {
                    db.child("items").child(item.id).child("category").setValue(updatedName)
                }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        db.child("categories").child(categoryId).removeValue()
    }

    fun updateItemAvailability(itemId: String, available: Boolean) {
        db.child("items").child(itemId).child("isAvailable").setValue(available)
    }

    fun addItem(item: MenuItem) {
        db.child("items").child(item.id).setValue(item)
    }

    fun deleteItem(itemId: String) {
        db.child("items").child(itemId).removeValue()
    }

    fun clearData() {
        stopListening()
        _categories.value = emptyList()
        _menuItems.value = emptyList()
    }
}
