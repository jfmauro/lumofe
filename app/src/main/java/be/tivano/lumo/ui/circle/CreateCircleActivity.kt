package be.tivano.lumo.ui.circle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityCreateCircleBinding
import be.tivano.lumo.model.CircleCreationDraft
import be.tivano.lumo.model.CreateCircleRequest
import be.tivano.lumo.ui.MainActivity
import be.tivano.lumo.util.CircleDraftManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CreateCircleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCircleBinding
    private lateinit var tokenManager: TokenManager

    private var selectedSuggestion: String = ""
    private var nameCheckJob: Job? = null

    private val draftHandler = Handler(Looper.getMainLooper())
    private val draftSaveRunnable = Runnable { saveDraft() }

    companion object {
        const val SUGGESTION_FAMILY = "famille_nom"
        const val SUGGESTION_FRIENDS = "mes_proches"
        const val SUGGESTION_SUPPORT = "circle_support"
        const val SUGGESTION_CUSTOM = "personnaliser"

        private val CIRCLE_NAME_PATTERN = Regex("^[a-zA-ZÀ-ÿ0-9\\s\\-']{3,50}$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCircleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)

        setupSuggestions()
        setupNameField()
        setupDescriptionField()
        setupCreateButton()
        setupBackButton()
        offerDraftRestoreIfAvailable()
    }

    // ─── SUGGESTIONS ─────────────────────────────────────────────────────────

    private fun setupSuggestions() {
        lifecycleScope.launch {
            val fullName = tokenManager.getUserFirstName() ?: ""
            val lastName = extractLastName(fullName)
            val familyLabel = if (lastName.isNotBlank())
                getString(R.string.circle_suggestion_family_named, capitalize(lastName))
            else
                getString(R.string.circle_suggestion_family)

            binding.tvSuggestionFamily.text = familyLabel

            binding.cardSuggestionFamily.setOnClickListener {
                selectSuggestion(SUGGESTION_FAMILY)
                binding.etCircleName.setText(familyLabel)
                binding.etCircleDescription.hint = getString(
                    R.string.circle_description_hint_family, capitalize(lastName)
                )
            }
            binding.cardSuggestionFriends.setOnClickListener {
                selectSuggestion(SUGGESTION_FRIENDS)
                binding.etCircleName.setText(getString(R.string.circle_suggestion_friends))
                binding.etCircleDescription.hint = getString(R.string.circle_description_hint_friends)
            }
            binding.cardSuggestionSupport.setOnClickListener {
                selectSuggestion(SUGGESTION_SUPPORT)
                binding.etCircleName.setText(getString(R.string.circle_suggestion_support))
                binding.etCircleDescription.hint = getString(R.string.circle_description_hint_support)
            }
            binding.cardSuggestionCustom.setOnClickListener {
                selectSuggestion(SUGGESTION_CUSTOM)
                binding.etCircleName.setText("")
                binding.etCircleName.requestFocus()
                binding.etCircleDescription.hint = getString(R.string.circle_description_hint_custom)
            }
        }
    }

    private fun selectSuggestion(key: String) {
        selectedSuggestion = key
        val defaultStroke = ContextCompat.getColor(this, R.color.lumo_outline_variant)
        val selectedStroke = ContextCompat.getColor(this, R.color.lumo_primary)

        listOf(
            binding.cardSuggestionFamily,
            binding.cardSuggestionFriends,
            binding.cardSuggestionSupport,
            binding.cardSuggestionCustom
        ).forEach { card ->
            card.strokeColor = defaultStroke
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        }

        val selectedCard = when (key) {
            SUGGESTION_FAMILY -> binding.cardSuggestionFamily
            SUGGESTION_FRIENDS -> binding.cardSuggestionFriends
            SUGGESTION_SUPPORT -> binding.cardSuggestionSupport
            else -> binding.cardSuggestionCustom
        }
        selectedCard.strokeColor = selectedStroke
        selectedCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.lumo_primary_container)
        )

        scheduleDraftSave()
    }

    // ─── NAME FIELD ──────────────────────────────────────────────────────────

    private fun setupNameField() {
        binding.etCircleName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString().orEmpty()
                validateNameLocally(value)
                if (value.length >= 3 && isValidNameFormat(value)) {
                    scheduleNameCheck(value)
                } else {
                    nameCheckJob?.cancel()
                }
                scheduleDraftSave()
                updateCreateButtonState()
            }
        })
    }

    private fun validateNameLocally(name: String) {
        when {
            name.isEmpty() -> {
                binding.tilCircleName.error = null
                binding.tilCircleName.isErrorEnabled = false
            }
            name.length < 3 -> {
                binding.tilCircleName.error = getString(R.string.error_circle_name_too_short)
            }
            name.length > 50 -> {
                binding.tilCircleName.error = getString(R.string.error_circle_name_too_long)
            }
            !isValidNameFormat(name) -> {
                binding.tilCircleName.error = getString(R.string.error_circle_name_invalid_chars)
            }
            else -> {
                binding.tilCircleName.error = null
                binding.tilCircleName.isErrorEnabled = false
            }
        }
    }

    private fun isValidNameFormat(name: String): Boolean = CIRCLE_NAME_PATTERN.matches(name.trim())

    private fun scheduleNameCheck(name: String) {
        nameCheckJob?.cancel()
        nameCheckJob = lifecycleScope.launch {
            delay(500)
            checkNameAvailability(name)
        }
    }

    private suspend fun checkNameAvailability(name: String) {
        try {
            val response = RetrofitClient.apiService.checkCircleName(name)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && !body.available) {
                    binding.tilCircleName.error = getString(R.string.error_circle_name_already_exists)
                    updateCreateButtonState()
                }
            }
        } catch (e: Exception) {
            // Silent failure — backend validation on submit will catch it
        }
    }

    // ─── DESCRIPTION FIELD ───────────────────────────────────────────────────

    private fun setupDescriptionField() {
        binding.etCircleDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                binding.tvDescCounter.text = getString(R.string.circle_desc_counter, len)
                if (len > 200) {
                    binding.tilCircleDescription.error = getString(R.string.error_circle_desc_too_long)
                } else {
                    binding.tilCircleDescription.error = null
                    binding.tilCircleDescription.isErrorEnabled = false
                }
                scheduleDraftSave()
                updateCreateButtonState()
            }
        })
    }

    // ─── CREATE BUTTON ───────────────────────────────────────────────────────

    private fun setupCreateButton() {
        binding.btnCreateCircle.isEnabled = false
        binding.btnCreateCircle.setOnClickListener { performCreateCircle() }
    }

    private fun updateCreateButtonState() {
        val name = binding.etCircleName.text?.toString().orEmpty()
        val descLen = binding.etCircleDescription.text?.length ?: 0
        val nameValid = isValidNameFormat(name) && binding.tilCircleName.error == null && name.length >= 3
        val descValid = descLen <= 200
        binding.btnCreateCircle.isEnabled = nameValid && descValid
    }

    private fun performCreateCircle() {
        val name = binding.etCircleName.text?.toString()?.trim()
            ?.replace(Regex("\\s+"), " ").orEmpty()
        val description = binding.etCircleDescription.text?.toString()?.trim()
            ?.ifBlank { null }

        if (!isValidNameFormat(name)) return

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createCircle(
                    CreateCircleRequest(name = name, description = description)
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        CircleDraftManager.clearDraft(this@CreateCircleActivity)
                        navigateToMain()
                    }
                    response.code() == 409 -> {
                        binding.tilCircleName.error = getString(R.string.error_circle_name_already_exists)
                        setLoadingState(false)
                    }
                    response.code() == 401 -> {
                        Toast.makeText(
                            this@CreateCircleActivity,
                            getString(R.string.error_session_expired),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 400 -> {
                        Toast.makeText(
                            this@CreateCircleActivity,
                            getString(R.string.common_error_generic),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    else -> {
                        Toast.makeText(
                            this@CreateCircleActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateCircleActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_LONG
                ).show()
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnCreateCircle.isEnabled = !loading
        binding.btnCreateCircle.text = if (loading)
            getString(R.string.circle_btn_creating)
        else
            getString(R.string.circle_btn_create)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    // ─── BACK BUTTON ─────────────────────────────────────────────────────────

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    // ─── DRAFT ───────────────────────────────────────────────────────────────

    private fun scheduleDraftSave() {
        draftHandler.removeCallbacks(draftSaveRunnable)
        draftHandler.postDelayed(draftSaveRunnable, 3000L)
    }

    private fun saveDraft() {
        CircleDraftManager.saveDraft(
            this, CircleCreationDraft(
                timestamp = System.currentTimeMillis(),
                selectedSuggestion = selectedSuggestion,
                circleName = binding.etCircleName.text?.toString().orEmpty(),
                circleDescription = binding.etCircleDescription.text?.toString().orEmpty(),
                currentScreen = 2
            )
        )
    }

    private fun offerDraftRestoreIfAvailable() {
        val draft = CircleDraftManager.loadDraft(this) ?: return
        if (draft.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.draft_restore_title)
                .setMessage(R.string.draft_restore_message)
                .setPositiveButton(R.string.draft_restore_yes) { _, _ -> restoreDraft(draft) }
                .setNegativeButton(R.string.draft_restore_no) { _, _ ->
                    CircleDraftManager.clearDraft(this)
                }
                .show()
        }
    }

    private fun restoreDraft(draft: CircleCreationDraft) {
        binding.etCircleName.setText(draft.circleName)
        binding.etCircleDescription.setText(draft.circleDescription)
        if (draft.selectedSuggestion.isNotBlank()) {
            selectSuggestion(draft.selectedSuggestion)
        }
        updateCreateButtonState()
    }

    // ─── NAVIGATION ──────────────────────────────────────────────────────────

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private fun extractLastName(fullName: String): String {
        val parts = fullName.trim().split(" ")
        return if (parts.size >= 2) parts.last() else ""
    }

    private fun capitalize(s: String): String =
        s.lowercase().replaceFirstChar { it.uppercase() }

    override fun onDestroy() {
        super.onDestroy()
        draftHandler.removeCallbacks(draftSaveRunnable)
        nameCheckJob?.cancel()
    }
}
