package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── REQUEST ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("disclaimerAccepted") val disclaimerAccepted: Boolean,
    @SerializedName("countryCode") val countryCode: String
)

// ─── RESPONSE ───────────────────────────────────────────────────────────────

data class RegisterResponse(
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("prenom") val prenom: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("telephone") val telephone: String?,
    @SerializedName("token") val token: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("createdAt") val createdAt: String
)

data class CheckEmailResponse(
    @SerializedName("available") val available: Boolean
)

data class ApiErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String,
    @SerializedName("details") val details: Map<String, String>?,
    @SerializedName("timestamp") val timestamp: String?
)

// ─── DRAFT ──────────────────────────────────────────────────────────────────

data class OnboardingDraft(
    val timestamp: Long,
    val prenom: String = "",
    val nom: String = "",
    val email: String = "",
    val telephone: String = "",
    val disclaimerAccepted: Boolean = false,
    val currentScreen: Int = 0
)
