package com.evchargedreminder.data.remote

import com.evchargedreminder.data.remote.dto.ChargePointDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenChargeMapApi {

    @GET("poi")
    suspend fun getNearbyChargers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distanceKm: Int = 1,
        @Query("maxresults") maxResults: Int = 5,
        @Query("compact") compact: Boolean = true,
        @Query("verbose") verbose: Boolean = false
    ): List<ChargePointDto>
}
