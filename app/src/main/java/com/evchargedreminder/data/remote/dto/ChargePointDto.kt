package com.evchargedreminder.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChargePointDto(
    @Json(name = "AddressInfo") val addressInfo: AddressInfoDto?,
    @Json(name = "Connections") val connections: List<ConnectionDto>?
)

@JsonClass(generateAdapter = true)
data class AddressInfoDto(
    @Json(name = "Title") val title: String?,
    @Json(name = "AddressLine1") val addressLine1: String?
)

@JsonClass(generateAdapter = true)
data class ConnectionDto(
    @Json(name = "PowerKW") val powerKw: Double?,
    @Json(name = "ConnectionType") val connectionType: ConnectionTypeDto?
)

@JsonClass(generateAdapter = true)
data class ConnectionTypeDto(
    @Json(name = "Title") val title: String?
)
