package be.tivano.lumo.model

import com.google.gson.annotations.SerializedName

data class InvitationListResponse(
    @SerializedName("content")
    val content: List<InvitationResponse>,

    @SerializedName("totalElements")
    val totalElements: Int,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("last")
    val last: Boolean,

    @SerializedName("first")
    val first: Boolean
)
