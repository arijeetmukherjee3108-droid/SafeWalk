package com.example.safewalk.data.model

data class RouteRequest(
    val coordinates: List<List<Double>>
)

data class PredictResponse(
    val risk: Int,
    val level: String,
    val summary: List<String>,
    val segments: List<RouteSegment>
)

data class RouteSegment(
    val lat: Double,
    val lng: Double,
    val risk: Int,
    val reasons: List<String>
)
