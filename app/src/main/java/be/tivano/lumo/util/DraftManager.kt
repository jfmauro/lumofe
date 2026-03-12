package be.tivano.lumo.util

import android.content.Context
import android.content.SharedPreferences
import be.tivano.lumo.model.OnboardingDraft
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

object DraftManager {

    private const val PREFS_NAME = "onboarding_draft_prefs"
    private const val KEY_DRAFT = "onboarding_draft_v1"
    private val EXPIRY_MS = TimeUnit.DAYS.toMillis(7)
    private val gson = Gson()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDraft(context: Context, draft: OnboardingDraft) {
        val json = gson.toJson(draft.copy(timestamp = System.currentTimeMillis()))
        prefs(context).edit().putString(KEY_DRAFT, json).apply()
    }

    fun loadDraft(context: Context): OnboardingDraft? {
        val json = prefs(context).getString(KEY_DRAFT, null) ?: return null
        return try {
            val draft = gson.fromJson(json, OnboardingDraft::class.java)
            val age = System.currentTimeMillis() - draft.timestamp
            if (age > EXPIRY_MS) {
                clearDraft(context)
                null
            } else {
                draft
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearDraft(context: Context) {
        prefs(context).edit().remove(KEY_DRAFT).apply()
    }

    fun hasDraft(context: Context): Boolean = loadDraft(context) != null
}
