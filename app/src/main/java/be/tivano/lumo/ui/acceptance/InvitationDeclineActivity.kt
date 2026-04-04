package be.tivano.lumo.ui.acceptance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.databinding.ActivityInvitationDeclineBinding
import be.tivano.lumo.model.DeclineInvitationRequest
import kotlinx.coroutines.launch

class InvitationDeclineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvitationDeclineBinding

    private var invitationToken: String = ""
    private var circleName: String = ""
    private var inviterFirstName: String = ""
    private var selectedSuggestionId: String? = null

    companion object {
        const val EXTRA_TOKEN = "extra_invitation_token"
        const val EXTRA_CIRCLE_NAME = "extra_circle_name"
        const val EXTRA_INVITER_FIRST_NAME = "extra_inviter_first_name"

        private const val SUGGESTION_NOT_COMFORTABLE = "NOT_COMFORTABLE"
        private const val SUGGESTION_PREFER_OTHER = "PREFER_OTHER_MEANS"
        private const val SUGGESTION_CANNOT_COMMIT = "CANNOT_COMMIT"
        private const val SUGGESTION_OTHER = "OTHER"
    }

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationDeclineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        invitationToken = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()
        inviterFirstName = intent.getStringExtra(EXTRA_INVITER_FIRST_NAME).orEmpty()

        if (invitationToken.isBlank()) {
            finish()
            return
        }

        setupHeader()
        setupBackButton()
        setupSuggestionChips()
        setupReasonField()
        setupActionButtons()
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    private fun setupHeader() {
        binding.tvDeclineQuestion.text = getString(R.string.decline_question, circleName)
        binding.tvDeclineInfo.text = getString(R.string.decline_info, inviterFirstName)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupSuggestionChips() {
        val chips = listOf(
            binding.chipNotComfortable to SUGGESTION_NOT_COMFORTABLE,
            binding.chipPreferOther to SUGGESTION_PREFER_OTHER,
            binding.chipCannotCommit to SUGGESTION_CANNOT_COMMIT,
            binding.chipOther to SUGGESTION_OTHER
        )

        chips.forEach { (chip, suggestionId) ->
            chip.setOnClickListener {
                val isAlreadySelected = selectedSuggestionId == suggestionId
                selectedSuggestionId = if (isAlreadySelected) null else suggestionId

                chips.forEach { (c, id) ->
                    val isActive = selectedSuggestionId == id
                    c.backgroundTintList = if (isActive)
                        androidx.core.content.ContextCompat.getColorStateList(this, R.color.lumo_primary)
                    else
                        androidx.core.content.ContextCompat.getColorStateList(this, R.color.lumo_surface_variant)
                    c.setTextColor(
                        if (isActive)
                            androidx.core.content.ContextCompat.getColor(this, R.color.white)
                        else
                            androidx.core.content.ContextCompat.getColor(this, R.color.lumo_on_surface_muted)
                    )
                }

                if (!isAlreadySelected) {
                    val label = chip.text.toString()
                    binding.etReason.setText(if (suggestionId == SUGGESTION_OTHER) "" else label)
                    if (suggestionId == SUGGESTION_OTHER) {
                        binding.etReason.requestFocus()
                    }
                } else {
                    binding.etReason.setText("")
                }
            }
        }
    }

    private fun setupReasonField() {
        binding.etReason.doAfterTextChanged {
            val text = it?.toString().orEmpty()
            if (text.isNotBlank()) {
                selectedSuggestionId = null
                resetChipSelection()
            }
            val length = text.length
            if (length > 200) {
                binding.tilReason.error = getString(R.string.decline_reason_too_long)
            } else {
                binding.tilReason.error = null
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnConfirmDecline.setOnClickListener { performDecline() }
        binding.btnCancelDecline.setOnClickListener { finish() }
    }

    // ─── API CALL ─────────────────────────────────────────────────────────────

    private fun performDecline() {
        val rawReason = binding.etReason.text?.toString().orEmpty().trim()
        val reason = rawReason.ifBlank { null }

        if (reason != null && reason.length > 200) {
            binding.tilReason.error = getString(R.string.decline_reason_too_long)
            return
        }
        binding.tilReason.error = null

        hideError()
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.declineInvitation(
                    token = invitationToken,
                    request = DeclineInvitationRequest(declineReason = reason)
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        val body = response.body()!!
                        Toast.makeText(
                            this@InvitationDeclineActivity,
                            getString(
                                R.string.decline_success_message,
                                body.inviterFirstName,
                                circleName
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    response.code() == 400 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val msg = when {
                            errorBody.contains("DECLINE_REASON_TOO_LONG") ->
                                getString(R.string.decline_reason_too_long)
                            else ->
                                getString(R.string.register_error_400)
                        }
                        setLoadingState(false)
                        showError(msg)
                    }
                    response.code() == 404 -> {
                        setLoadingState(false)
                        showError(getString(R.string.landing_error_not_found))
                    }
                    response.code() == 409 -> {
                        setLoadingState(false)
                        showError(getString(R.string.landing_error_already_processed))
                    }
                    response.code() == 410 -> {
                        setLoadingState(false)
                        showError(getString(R.string.landing_error_expired))
                    }
                    else -> {
                        setLoadingState(false)
                        showError(getString(R.string.common_error_server))
                    }
                }
            } catch (e: Exception) {
                setLoadingState(false)
                showError(getString(R.string.common_error_network))
            }
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private fun resetChipSelection() {
        listOf(
            binding.chipNotComfortable,
            binding.chipPreferOther,
            binding.chipCannotCommit,
            binding.chipOther
        ).forEach { chip ->
            chip.backgroundTintList =
                androidx.core.content.ContextCompat.getColorStateList(this, R.color.lumo_surface_variant)
            chip.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, R.color.lumo_on_surface_muted)
            )
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnConfirmDecline.isEnabled = !loading
        binding.btnCancelDecline.isEnabled = !loading
        binding.btnConfirmDecline.text = if (loading)
            getString(R.string.common_loading)
        else
            getString(R.string.decline_btn_confirm)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.errorBanner.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorBanner.visibility = View.GONE
    }
}
