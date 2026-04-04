package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class DeclineInvitationResponse(
    @SerializedName("invitationToken")
    val invitationToken: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("declinedAt")
    val declinedAt: String,

    @SerializedName("declineReason")
    val declineReason: String? = null,

    @SerializedName("reinviteAfter")
    val reinviteAfter: String,

    @SerializedName("inviterFirstName")
    val inviterFirstName: String
)
