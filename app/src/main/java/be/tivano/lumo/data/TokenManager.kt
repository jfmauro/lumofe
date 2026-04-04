package be.tivano.lumo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

        // ─── US-0.2.4 — Active circle ────────────────────────────────────────
        private val CIRCLE_ID_KEY = stringPreferencesKey("active_circle_id")
        private val CIRCLE_NAME_KEY = stringPreferencesKey("active_circle_name")

        // ─── US-0.3.2 — Circle role (creator vs member) ──────────────────────
        private val IS_CIRCLE_CREATOR_KEY = booleanPreferencesKey("is_circle_creator")
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

    // ─── Active circle ────────────────────────────────────────────────────────

    suspend fun saveActiveCircle(circleId: String, circleName: String) {
        context.dataStore.edit { preferences ->
            preferences[CIRCLE_ID_KEY] = circleId
            preferences[CIRCLE_NAME_KEY] = circleName
        }
    }

    suspend fun getActiveCircleId(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[CIRCLE_ID_KEY]
    }

    suspend fun getActiveCircleName(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[CIRCLE_NAME_KEY]
    }

    fun getActiveCircleIdSync(): String? {
        return try {
            kotlinx.coroutines.runBlocking { getActiveCircleId() }
        } catch (e: Exception) {
            null
        }
    }

    fun getActiveCircleNameSync(): String? {
        return try {
            kotlinx.coroutines.runBlocking { getActiveCircleName() }
        } catch (e: Exception) {
            null
        }
    }

    // ─── Circle role ──────────────────────────────────────────────────────────

    /**
     * Persists whether the current user is the creator of their active circle.
     * Must be called immediately after account creation or invitation acceptance.
     * Cleared automatically by [clearAll] on logout.
     */
    suspend fun saveIsCircleCreator(isCreator: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_CIRCLE_CREATOR_KEY] = isCreator
        }
    }

    /**
     * Returns true only if the user explicitly saved the CREATOR role.
     * Defaults to false when the key is absent (safe default: restrict access).
     */
    suspend fun isCircleCreator(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[IS_CIRCLE_CREATOR_KEY] ?: false
    }

    fun isCircleCreatorSync(): Boolean {
        return try {
            kotlinx.coroutines.runBlocking { isCircleCreator() }
        } catch (e: Exception) {
            false
        }
    }

    // ─── Clear ────────────────────────────────────────────────────────────────

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}