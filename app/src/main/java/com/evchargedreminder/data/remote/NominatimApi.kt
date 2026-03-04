package com.evchargedreminder.data.remote

import com.evchargedreminder.data.remote.dto.NominatimResult
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NominatimApi {

    @Headers("User-Agent: EVChargedReminder/1.0")
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json",
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressDetails: Int = 0
    ): NominatimResult
}
