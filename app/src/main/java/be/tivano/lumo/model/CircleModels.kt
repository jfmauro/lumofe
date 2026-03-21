package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

// ─── CIRCLE REQUEST ─────────────────────────────────────────────────────────

data class CreateCircleRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?
)

// ─── CIRCLE RESPONSE ────────────────────────────────────────────────────────

data class CreateCircleResponse(
    @SerializedName("circleId") val circleId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("createdBy") val createdBy: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("memberCount") val memberCount: Int,
    @SerializedName("status") val status: String
)

data class CheckCircleNameResponse(
    @SerializedName("name") val name: String,
    @SerializedName("available") val available: Boolean,
    @SerializedName("existingCircleId") val existingCircleId: String?
)

// ─── CIRCLE DRAFT ───────────────────────────────────────────────────────────

data class CircleCreationDraft(
    val timestamp: Long = 0L,
    val selectedSuggestion: String = "",
    val circleName: String = "",
    val circleDescription: String = "",
    val currentScreen: Int = 2
) {
    fun isNotEmpty(): Boolean = circleName.isNotBlank()

    fun isExpired(): Boolean {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - timestamp > sevenDaysMs
    }

    fun sanitized(): CircleCreationDraft = copy(
        circleName = circleName.trim().replace(Regex("\\s+"), " "),
        circleDescription = circleDescription.trim()
    )
}
