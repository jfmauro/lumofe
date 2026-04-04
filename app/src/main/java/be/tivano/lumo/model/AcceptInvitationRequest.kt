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

    @SerializedName("preferredCheckinTime")
    val preferredCheckinTime: String = "20:00",

    @SerializedName("responseWindowHours")
    val responseWindowHours: Int = 8
)
