package be.tivano.lumo.data

import be.tivano.lumo.model.CheckEmailResponse
import be.tivano.lumo.model.RegisterRequest
import be.tivano.lumo.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/v1/users/check-email")
    suspend fun checkEmail(@Query("email") email: String): Response<CheckEmailResponse>
}
