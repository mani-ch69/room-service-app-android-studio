package com.example.roomservice.data.model

import java.util.UUID

data class CallRequest(
    var id: String = UUID.randomUUID().toString(),
    var roomNumber: String = "",
    var status: CallStatus = CallStatus.PENDING,
    var timestamp: Long = System.currentTimeMillis(),
    var assignedStaffId: String? = null,
    var assignedStaffName: String? = null
)

enum class CallStatus {
    PENDING,
    RECEIVED,
    ATTENDED,
    CANCELLED
}