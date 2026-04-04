package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class DeclineReasonsResponse(
    @SerializedName("suggestions")
    val suggestions: List<DeclineReasonSuggestion>
)

data class DeclineReasonSuggestion(
    @SerializedName("id")
    val id: String,

    @SerializedName("label")
    val label: String
)
