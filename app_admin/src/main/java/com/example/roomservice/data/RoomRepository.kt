package com.example.roomservice.data

import com.example.roomservice.data.model.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RoomRepository {
    private var db = FirebaseDatabase.getInstance().getReference("rooms")
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("rooms")
        db.keepSynced(true)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Optimization: Use sequence for mapping large lists
                val list = snapshot.children.asSequence()
                    .mapNotNull { it.getValue(Room::class.java) }
                    .toList()
                _rooms.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun addRoom(room: Room) {
        if (room.roomNumber.isBlank()) return
        db.child(room.roomNumber).setValue(room)
    }

    fun updateRoom(updatedRoom: Room) {
        db.child(updatedRoom.roomNumber).setValue(updatedRoom)
    }

    fun deleteRoom(roomNumber: String) {
        db.child(roomNumber).removeValue()
    }

    fun toggleRoomAvailability(roomNumber: String) {
        val room = _rooms.value.find { it.roomNumber == roomNumber }
        room?.let {
            db.child(roomNumber).child("isAvailable").setValue(!it.isAvailable)
        }
    }

    fun clearData() {
        stopListening()
        _rooms.value = emptyList()
    }
}
