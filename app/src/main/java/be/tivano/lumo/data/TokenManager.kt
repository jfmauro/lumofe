package be.tivano.lumo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "lumo_auth_preferences"
        )

        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_FIRSTNAME_KEY = stringPreferencesKey("user_firstname")
        private val USER_LASTNAME_KEY = stringPreferencesKey("user_lastname")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[TOKEN_KEY]
    }

    fun getTokenSync(): String? {
        return try {
            kotlinx.coroutines.runBlocking { getToken() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUser(userId: String, fullName: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_FIRSTNAME_KEY] = fullName
            preferences[USER_EMAIL_KEY] = email
        }
    }

    suspend fun getUserId(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_ID_KEY]
    }

    suspend fun getUserFirstName(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_FIRSTNAME_KEY]
    }

    suspend fun getUserEmail(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_EMAIL_KEY]
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null

    fun isLoggedInSync(): Boolean {
        return try {
            kotlinx.coroutines.runBlocking { isLoggedIn() }
        } catch (e: Exception) {
            false
        }
    }

    fun isLoggedInFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY] != null
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
