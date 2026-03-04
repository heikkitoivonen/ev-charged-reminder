package com.evchargedreminder.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NominatimResult(
    @Json(name = "display_name") val displayName: String?
)
