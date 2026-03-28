package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class InvitationResponse(
    @SerializedName("invitationId")
    val invitationId: String,

    @SerializedName("circleId")
    val circleId: String,

    @SerializedName("inviterId")
    val inviterId: String?,

    @SerializedName("inviteeEmail")
    val inviteeEmail: String,

    @SerializedName("personalMessage")
    val personalMessage: String?,

    @SerializedName("channel")
    val channel: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("sentAt")
    val sentAt: String?,

    @SerializedName("expiresAt")
    val expiresAt: String?,

    @SerializedName("emailDeliveryStatus")
    val emailDeliveryStatus: String?,

    @SerializedName("resendCount")
    val resendCount: Int = 0,

    @SerializedName("emailOpenedAt")
    val emailOpenedAt: String? = null,

    @SerializedName("acceptedAt")
    val acceptedAt: String? = null
)
