package be.tivano.lumo.data

import be.tivano.lumo.model.AcceptInvitationRequest
import be.tivano.lumo.model.AcceptInvitationResponse
import be.tivano.lumo.model.CheckCircleNameResponse
import be.tivano.lumo.model.CircleSettingsResponse
import be.tivano.lumo.model.CreateCircleRequest
import be.tivano.lumo.model.CreateCircleResponse
import be.tivano.lumo.model.CreateCircleSettingsRequest
import be.tivano.lumo.model.CreatorConsentRequest
import be.tivano.lumo.model.CreatorConsentResponse
import be.tivano.lumo.model.DeclineInvitationRequest
import be.tivano.lumo.model.DeclineInvitationResponse
import be.tivano.lumo.model.DeclineReasonsResponse
import be.tivano.lumo.model.InvitationLandingResponse
import be.tivano.lumo.model.InvitationListResponse
import be.tivano.lumo.model.InvitationRequest
import be.tivano.lumo.model.InvitationResponse
import be.tivano.lumo.model.InvitationStatisticsResponse
import be.tivano.lumo.model.RegisterRequest
import be.tivano.lumo.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/circles")
    suspend fun createCircle(@Body request: CreateCircleRequest): Response<CreateCircleResponse>

    @GET("api/v1/circles/check-name")
    suspend fun checkCircleName(@Query("name") name: String): Response<CheckCircleNameResponse>

    @POST("api/v1/circles/{circleId}/settings")
    suspend fun createCircleSettings(
        @Path("circleId") circleId: String,
        @Body request: CreateCircleSettingsRequest
    ): Response<CircleSettingsResponse>

    @POST("api/v1/circles/{circleId}/creator-consent")
    suspend fun acceptCreatorConsent(
        @Path("circleId") circleId: String,
        @Body request: CreatorConsentRequest
    ): Response<CreatorConsentResponse>

    // ─── US-0.2.1 — Email Invitation ─────────────────────────────────────────

    @POST("api/v1/circles/{circleId}/invitations")
    suspend fun sendInvitation(
        @Path("circleId") circleId: String,
        @Body request: InvitationRequest
    ): Response<InvitationResponse>

    @GET("api/v1/circles/{circleId}/invitations")
    suspend fun getInvitations(
        @Path("circleId") circleId: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "sentAt,desc"
    ): Response<InvitationListResponse>

    @DELETE("api/v1/circles/{circleId}/invitations/{invitationId}")
    suspend fun revokeInvitation(
        @Path("circleId") circleId: String,
        @Path("invitationId") invitationId: String
    ): Response<InvitationResponse>

    // ─── US-0.2.4 — Invitation Tracking ──────────────────────────────────────

    @GET("api/v1/circles/{circleId}/invitations/statistics")
    suspend fun getInvitationStatistics(
        @Path("circleId") circleId: String
    ): Response<InvitationStatisticsResponse>

    @POST("api/v1/circles/{circleId}/invitations/{invitationId}/resend")
    suspend fun resendInvitation(
        @Path("circleId") circleId: String,
        @Path("invitationId") invitationId: String
    ): Response<InvitationResponse>

    // ─── US-0.3.1 — Invitation Landing (public, no JWT) ──────────────────────

    @GET("api/v1/invitations/{token}/landing")
    suspend fun getInvitationLanding(
        @Path("token") token: String
    ): Response<InvitationLandingResponse>

    // ─── US-0.3.2 — Accept Invitation / Guest Onboarding (public, no JWT) ────

    @POST("api/v1/invitations/{token}/accept")
    suspend fun acceptInvitation(
        @Path("token") token: String,
        @Body request: AcceptInvitationRequest
    ): Response<AcceptInvitationResponse>

    // ─── US-0.3.3 — Decline Invitation (public, no JWT) ──────────────────────

    @POST("api/v1/invitations/{token}/decline")
    suspend fun declineInvitation(
        @Path("token") token: String,
        @Body request: DeclineInvitationRequest
    ): Response<DeclineInvitationResponse>

    @GET("api/v1/invitations/decline-reasons")
    suspend fun getDeclineReasons(): Response<DeclineReasonsResponse>
}
