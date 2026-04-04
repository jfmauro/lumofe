package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class DeclineInvitationRequest(
    @SerializedName("declineReason")
    val declineReason: String? = null
)
