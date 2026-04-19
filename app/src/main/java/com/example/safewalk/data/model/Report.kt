package com.example.safewalk.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Report(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val suspectName: String = "",
    val timestamp: Long = 0,
    val txHash: String? = null
)
