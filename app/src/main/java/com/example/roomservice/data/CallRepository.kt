package com.example.roomservice.data

import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.CallStatus
import com.example.roomservice.data.model.NotificationType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallRepository {
    private var db = FirebaseDatabase.getInstance().getReference("calls")
    private val _calls = MutableStateFlow<List<CallRequest>>(emptyList())
    val calls: StateFlow<List<CallRequest>> = _calls.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("calls")
        db.keepSynced(true)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(CallRequest::class.java) }
                
                // Notify for new Pending calls
                list.forEach { call ->
                    if (call.status == CallStatus.PENDING) {
                        // Using a simple check: if we don't have this call in current list yet
                        if (_calls.value.none { it.id == call.id }) {
                            NotificationRepository.addNotification(
                                title = "Service Call",
                                message = "Room ${call.roomNumber} is requesting assistance",
                                type = NotificationType.CALL_STAFF,
                                roomNumber = call.roomNumber
                            )
                        }
                    }
                }

                _calls.value = list
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
        _calls.value = emptyList()
    }

    fun makeCall(roomNumber: String) {
        val newCall = CallRequest(roomNumber = roomNumber)
        db.child(newCall.id).setValue(newCall)
    }

    fun attendCall(callId: String) {
        db.child(callId).child("status").setValue(CallStatus.ATTENDED)
    }

    fun assignStaffToCall(callId: String, staffId: String, staffName: String) {
        db.child(callId).child("assignedStaffId").setValue(staffId)
        db.child(callId).child("assignedStaffName").setValue(staffName)
        db.child(callId).child("status").setValue(CallStatus.ATTENDED)
    }

    fun unassignStaffFromAllCalls(staffId: String) {
        _calls.value.forEach { call ->
            if (call.assignedStaffId == staffId && call.status != CallStatus.ATTENDED) {
                db.child(call.id).child("assignedStaffId").removeValue()
                db.child(call.id).child("assignedStaffName").removeValue()
            }
        }
    }
    
    fun cancelCall(roomNumber: String) {
        val pendingCallId = _calls.value.find { it.roomNumber == roomNumber && it.status == CallStatus.PENDING }?.id
        pendingCallId?.let { db.child(it).removeValue() }
    }
}
