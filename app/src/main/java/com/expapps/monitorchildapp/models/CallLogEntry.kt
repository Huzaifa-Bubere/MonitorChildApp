package com.expapps.monitorchildapp.models

data class CallLogEntry(
    val callId: String?,
    val phoneNumber: String?,
    val callType: Int?,
    val callDate: Long?,
    val callDuration: Long?
)
