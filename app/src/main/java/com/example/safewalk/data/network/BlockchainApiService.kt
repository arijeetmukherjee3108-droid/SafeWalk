package com.example.safewalk.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Web3 Blockchain API for incident reports.
 * Deployed alongside the ML model at the same base URL.
 *
 * Endpoints from OpenAPI spec:
 *   POST /api/reports/submit  (multipart/form-data)
 *   POST /api/reports/resolve (JSON)
 */
interface BlockchainApiService {

    @Multipart
    @POST("api/reports/submit")
    suspend fun submitReport(
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("incident_type") incidentType: RequestBody,
        @Part("description") description: RequestBody,
        @Part("suspect_name") suspectName: RequestBody,
        @Part evidenceFile: MultipartBody.Part
    ): BlockchainReportResponse

    @POST("api/reports/resolve")
    suspend fun resolveReport(@Body request: ResolveReportRequest): Any
}

data class BlockchainReportResponse(
    val status: String? = null,
    val blockchain_receipt: String? = null,
    val ipfs_url: String? = null,
    val resolution_secret: String? = null,
    val message: String? = null
)

data class ResolveReportRequest(
    val report_id: Int,
    val secret: String
)
