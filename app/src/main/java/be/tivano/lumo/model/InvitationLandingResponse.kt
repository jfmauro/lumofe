package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class InvitationLandingResponse(
    @SerializedName("invitationToken")
    val invitationToken: String,

    @SerializedName("circleId")
    val circleId: String,

    @SerializedName("circleName")
    val circleName: String,

    @SerializedName("inviterFirstName")
    val inviterFirstName: String,

    @SerializedName("inviterLastName")
    val inviterLastName: String,

    @SerializedName("currentMemberCount")
    val currentMemberCount: Int,

    @SerializedName("expiresAt")
    val expiresAt: String,

    @SerializedName("channel")
    val channel: String
)
