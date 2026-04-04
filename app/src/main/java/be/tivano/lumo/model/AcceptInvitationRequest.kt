package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class AcceptInvitationRequest(
    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("consentAccepted")
    val consentAccepted: Boolean,

    // null = guest opted out of personal check-in tracking
    @SerializedName("preferredCheckinTime")
    val preferredCheckinTime: String? = null,

    // null = guest opted out of personal check-in tracking
    @SerializedName("responseWindowHours")
    val responseWindowHours: Int? = null
)