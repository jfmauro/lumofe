package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── REQUEST ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("disclaimerAccepted") val disclaimerAccepted: Boolean
)

// ─── RESPONSE ───────────────────────────────────────────────────────────────

data class RegisterResponse(
    @SerializedName("token") val token: String,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: RegisteredUser
)

data class RegisteredUser(
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String
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
    val timestamp: Long = 0L,
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val phone: String = "",
    val countryCode: String = "BE",
    val disclaimerAccepted: Boolean = false,
    val currentScreen: Int = 0
) {
    fun sanitized(): OnboardingDraft = copy(
        firstname = firstname.orSafe(),
        lastname = lastname.orSafe(),
        email = email.orSafe(),
        phone = phone.orSafe(),
        countryCode = countryCode.orSafe("BE")
    )

    fun isNotEmpty(): Boolean =
        firstname.isNotBlank() || lastname.isNotBlank() || email.isNotBlank()

    private fun String?.orSafe(default: String = ""): String = this?.trim() ?: default
}