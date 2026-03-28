package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class InvitationRequest(
    @SerializedName("inviteeEmail")
    val inviteeEmail: String,

    @SerializedName("personalMessage")
    val personalMessage: String?,

    @SerializedName("channel")
    val channel: String = "EMAIL"
)
