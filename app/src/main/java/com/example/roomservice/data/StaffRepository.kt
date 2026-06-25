package com.example.roomservice.data

import com.example.roomservice.data.model.Staff
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StaffRepository {
    private var db = FirebaseDatabase.getInstance().getReference("staff")
    private val _staffList = MutableStateFlow<List<Staff>>(emptyList())
    val staffList: StateFlow<List<Staff>> = _staffList.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("staff")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Staff::class.java) }
                _staffList.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun addStaff(staff: Staff) {
        db.child(staff.id).setValue(staff)
        // Also save to global staff lookup for login
        FirebaseDatabase.getInstance().getReference("staff_lookup").child(staff.phone).setValue(staff)
    }

    fun deleteStaff(staffId: String) {
        val staff = _staffList.value.find { it.id == staffId }
        staff?.let { FirebaseDatabase.getInstance().getReference("staff_lookup").child(it.phone).removeValue() }
        db.child(staffId).removeValue()
    }

    fun updateStaffAvailability(staffId: String, available: Boolean) {
        db.child(staffId).child("isAvailable").setValue(available)
    }

    fun clearData() {
        stopListening()
        _staffList.value = emptyList()
    }
}
