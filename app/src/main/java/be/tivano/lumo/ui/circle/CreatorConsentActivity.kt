package be.tivano.lumo.ui.circle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityCreatorConsentBinding
import be.tivano.lumo.model.CreatorConsentRequest
import be.tivano.lumo.ui.invitation.InviteByEmailActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

class CreatorConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatorConsentBinding
    private lateinit var tokenManager: TokenManager

    private var circleId: String = ""
    private var circleName: String = ""
    private var checkboxesUnlocked = false

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"
        const val EXTRA_CIRCLE_NAME = "extra_circle_name"

        private const val DISCLAIMER_VERSION = "v1.0.0"
        private const val SUCCESS_REDIRECT_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatorConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        circleId = intent.getStringExtra(EXTRA_CIRCLE_ID) ?: run {
            finish()
            return
        }
        circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()

        setupScrollListener()
        setupCheckboxListeners()
        setupButtonListener()
    }

    // ─── SCROLL DETECTION ────────────────────────────────────────────────────

    private fun setupScrollListener() {
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (!checkboxesUnlocked) {
                val scrollView = binding.scrollView
                val child = scrollView.getChildAt(0)
                if (child != null) {
                    val contentHeight = child.height
                    val scrollViewHeight = scrollView.height
                    if (scrollY + scrollViewHeight >= contentHeight - 50) {
                        unlockCheckboxes()
                    }
                }
            }
        }
    }

    private fun unlockCheckboxes() {
        checkboxesUnlocked = true
        binding.cbResponsibility.isEnabled = true
        binding.cbEmergency.isEnabled = true
        binding.cbInformMembers.isEnabled = true
    }

    // ─── CHECKBOXES ──────────────────────────────────────────────────────────

    private fun setupCheckboxListeners() {
        binding.cbResponsibility.isEnabled = false
        binding.cbEmergency.isEnabled = false
        binding.cbInformMembers.isEnabled = false

        val updateButton = {
            binding.btnAcceptCreate.isEnabled =
                binding.cbResponsibility.isChecked &&
                        binding.cbEmergency.isChecked &&
                        binding.cbInformMembers.isChecked
        }
        binding.cbResponsibility.setOnCheckedChangeListener { _, _ -> updateButton() }
        binding.cbEmergency.setOnCheckedChangeListener { _, _ -> updateButton() }
        binding.cbInformMembers.setOnCheckedChangeListener { _, _ -> updateButton() }
        binding.btnAcceptCreate.isEnabled = false
    }

    // ─── SUBMIT ──────────────────────────────────────────────────────────────

    private fun setupButtonListener() {
        binding.btnAcceptCreate.setOnClickListener { performConsent() }
    }

    private fun performConsent() {
        if (!binding.cbResponsibility.isChecked ||
            !binding.cbEmergency.isChecked ||
            !binding.cbInformMembers.isChecked
        ) {
            Toast.makeText(
                this,
                getString(R.string.creator_consent_error_validation),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        setLoadingState(true)

        val consentedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.acceptCreatorConsent(
                    circleId = circleId,
                    request = CreatorConsentRequest(
                        responsibilityAccepted = true,
                        emergencyUnderstood = true,
                        informMembersAccepted = true,
                        disclaimerVersion = DISCLAIMER_VERSION,
                        consentedAt = consentedAt
                    )
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        onConsentSuccess()
                    }
                    response.code() == 409 -> {
                        // 409 = consent already recorded — treat as success
                        onConsentSuccess()
                    }
                    response.code() == 400 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.creator_consent_error_validation),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 401 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.error_session_expired),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.creator_consent_error_forbidden),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 404 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.creator_consent_error_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 422 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.creator_consent_error_unprocessable),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    else -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreatorConsentActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_LONG
                ).show()
                setLoadingState(false)
            }
        }
    }

    // ─── SUCCESS ─────────────────────────────────────────────────────────────

    private fun onConsentSuccess() {
        Toast.makeText(
            this,
            getString(R.string.creator_consent_success),
            Toast.LENGTH_LONG
        ).show()

        lifecycleScope.launch {
            // Persist active circle so MainActivity can access it from the drawer
            tokenManager.saveActiveCircle(circleId, circleName)
            // Mark this user as circle creator — controls invitations visibility
            tokenManager.saveIsCircleCreator(true)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToInvite()
        }, SUCCESS_REDIRECT_DELAY_MS)
    }

    private fun navigateToInvite() {
        val intent = Intent(this, InviteByEmailActivity::class.java).apply {
            putExtra(InviteByEmailActivity.EXTRA_CIRCLE_ID, circleId)
            putExtra(InviteByEmailActivity.EXTRA_CIRCLE_NAME, circleName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    // ─── LOADING STATE ────────────────────────────────────────────────────────

    private fun setLoadingState(loading: Boolean) {
        binding.btnAcceptCreate.isEnabled = !loading
        binding.btnAcceptCreate.text = if (loading)
            getString(R.string.creator_consent_btn_loading)
        else
            getString(R.string.creator_consent_btn_accept)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}