package be.tivano.lumo.data

import be.tivano.lumo.model.CheckCircleNameResponse
import be.tivano.lumo.model.CheckEmailResponse
import be.tivano.lumo.model.CreateCircleRequest
import be.tivano.lumo.model.CreateCircleResponse
import be.tivano.lumo.model.RegisterRequest
import be.tivano.lumo.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/circles")
    suspend fun createCircle(@Body request: CreateCircleRequest): Response<CreateCircleResponse>

    @GET("api/v1/circles/check-name")
    suspend fun checkCircleName(@Query("name") name: String): Response<CheckCircleNameResponse>
}
