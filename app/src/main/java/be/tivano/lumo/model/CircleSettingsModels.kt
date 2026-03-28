package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── REQUEST ─────────────────────────────────────────────────────────────────

data class CreateCircleSettingsRequest(
    @SerializedName("targetSize") val targetSize: String,
    @SerializedName("checkinMode") val checkinMode: String,
    @SerializedName("protectionLevel") val protectionLevel: String,
    @SerializedName("metricsVisible") val metricsVisible: Boolean
)

// ─── RESPONSE ────────────────────────────────────────────────────────────────

data class CircleSettingsResponse(
    @SerializedName("settingsId") val settingsId: String,
    @SerializedName("circleId") val circleId: String,
    @SerializedName("targetSize") val targetSize: String,
    @SerializedName("checkinMode") val checkinMode: String,
    @SerializedName("protectionLevel") val protectionLevel: String,
    @SerializedName("metricsVisible") val metricsVisible: Boolean,
    @SerializedName("createdAt") val createdAt: String
)
