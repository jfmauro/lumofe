package be.tivano.lumo.data

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private val PUBLIC_PATHS = listOf(
            "/api/v1/users/register",
            "/api/v1/users/check-email",
            "/api/v1/invitations/"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        val isPublic = PUBLIC_PATHS.any { url.contains(it) }
        if (isPublic) {
            Log.d(TAG, "Public endpoint, skipping auth: $url")
            return chain.proceed(request)
        }

        val token = runBlocking { tokenManager.getToken() }

        if (token.isNullOrEmpty()) {
            Log.w(TAG, "No token available for protected URL: $url")
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
