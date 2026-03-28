package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class InvitationStatisticsResponse(
    @SerializedName("circleId")
    val circleId: String,

    @SerializedName("totalSent")
    val totalSent: Int,

    @SerializedName("totalPending")
    val totalPending: Int,

    @SerializedName("totalAccepted")
    val totalAccepted: Int,

    @SerializedName("totalDeclined")
    val totalDeclined: Int,

    @SerializedName("totalExpired")
    val totalExpired: Int,

    @SerializedName("totalRevoked")
    val totalRevoked: Int,

    @SerializedName("acceptanceRate")
    val acceptanceRate: Double,

    @SerializedName("currentMemberCount")
    val currentMemberCount: Int
)
