package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── REQUEST ─────────────────────────────────────────────────────────────────

data class CreatorConsentRequest(
    @SerializedName("responsibilityAccepted") val responsibilityAccepted: Boolean,
    @SerializedName("emergencyUnderstood") val emergencyUnderstood: Boolean,
    @SerializedName("informMembersAccepted") val informMembersAccepted: Boolean,
    @SerializedName("disclaimerVersion") val disclaimerVersion: String,
    @SerializedName("consentedAt") val consentedAt: String
)

// ─── RESPONSE ────────────────────────────────────────────────────────────────

data class CreatorConsentResponse(
    @SerializedName("consentId") val consentId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("circleId") val circleId: String,
    @SerializedName("consentType") val consentType: String,
    @SerializedName("disclaimerVersion") val disclaimerVersion: String,
    @SerializedName("consentedAt") val consentedAt: String,
    @SerializedName("recordedAt") val recordedAt: String,
    @SerializedName("circle") val circle: ConsentCircleInfo
)

data class ConsentCircleInfo(
    @SerializedName("circleId") val circleId: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("activatedAt") val activatedAt: String?
)
