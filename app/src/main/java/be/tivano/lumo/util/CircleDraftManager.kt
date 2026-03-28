package be.tivano.lumo.util

import android.content.Context
import be.tivano.lumo.model.CircleCreationDraft
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object CircleDraftManager {

    private const val PREFS_NAME = "lumo_circle_draft"
    private const val KEY_DRAFT = "circle_creation_draft_v1"

    private val gson = Gson()

    fun saveDraft(context: Context, draft: CircleCreationDraft) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DRAFT, gson.toJson(draft)).apply()
    }

    fun loadDraft(context: Context): CircleCreationDraft? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DRAFT, null) ?: return null
        return try {
            val draft = gson.fromJson(json, CircleCreationDraft::class.java)
            if (draft == null || draft.isExpired()) {
                clearDraft(context)
                null
            } else {
                draft.sanitized()
            }
        } catch (e: JsonSyntaxException) {
            clearDraft(context)
            null
        }
    }

    fun clearDraft(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DRAFT).apply()
    }
}
