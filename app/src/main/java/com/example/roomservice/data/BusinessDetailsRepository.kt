package com.example.roomservice.data

import com.example.roomservice.data.model.BusinessDetails
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BusinessDetailsRepository {
    private var db = FirebaseDatabase.getInstance().getReference("business_details")
    private val _details = MutableStateFlow(BusinessDetails())
    val details: StateFlow<BusinessDetails> = _details.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("business_details")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(BusinessDetails::class.java)?.let {
                    _details.value = it
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun clearData() {
        stopListening()
        _details.value = BusinessDetails()
    }

    fun updateDetails(newDetails: BusinessDetails, onComplete: (Boolean) -> Unit) {
        db.setValue(newDetails).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _details.value = newDetails
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }
}
