package com.example.safewalk.data.network

import com.example.safewalk.data.model.PredictResponse
import com.example.safewalk.data.model.RouteRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface MLApiService {
    @POST("predict")
    suspend fun predictRisk(@Body request: RouteRequest): PredictResponse
}
