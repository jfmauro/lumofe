package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class AcceptInvitationResponse(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("circleId")
    val circleId: String,

    @SerializedName("circleName")
    val circleName: String,

    @SerializedName("membershipId")
    val membershipId: String,

    @SerializedName("joinedAt")
    val joinedAt: String,

    @SerializedName("currentMemberCount")
    val currentMemberCount: Int,

    @SerializedName("jwtToken")
    val jwtToken: String,

    @SerializedName("jwtExpiresAt")
    val jwtExpiresAt: String
)
