package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── REQUEST ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("prenom") val prenom: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("email") val email: String,
    @SerializedName("telephone") val telephone: String?,
    @SerializedName("disclaimerAcceptedAt") val disclaimerAcceptedAt: String,
    @SerializedName("disclaimerVersion") val disclaimerVersion: String
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
