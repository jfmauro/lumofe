package be.tivano.lumo.ui.circle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import be.tivano.lumo.ui.invitation.InviteByEmailActivity
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.databinding.ActivityCreatorConsentBinding
import be.tivano.lumo.model.CreatorConsentRequest
import be.tivano.lumo.ui.MainActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

class CreatorConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatorConsentBinding

    private var circleId: String = ""
    private var checkboxesUnlocked = false

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"

        private const val DISCLAIMER_VERSION = "v1.0.0"
        private const val SUCCESS_REDIRECT_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatorConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        circleId = intent.getStringExtra(EXTRA_CIRCLE_ID) ?: run {
            finish()
            return
        }

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
        binding.tvConsentHint.visibility = View.GONE
    }

    // ─── CHECKBOX VALIDATION ─────────────────────────────────────────────────

    private fun setupCheckboxListeners() {
        val listener = { _: android.widget.CompoundButton, _: Boolean ->
            updateButtonState()
        }
        binding.cbResponsibility.setOnCheckedChangeListener(listener)
        binding.cbEmergency.setOnCheckedChangeListener(listener)
        binding.cbInformMembers.setOnCheckedChangeListener(listener)
    }

    private fun updateButtonState() {
        val allChecked = binding.cbResponsibility.isChecked
                && binding.cbEmergency.isChecked
                && binding.cbInformMembers.isChecked
        binding.btnAcceptCreate.isEnabled = allChecked
    }

    // ─── API CALL ─────────────────────────────────────────────────────────────

    private fun setupButtonListener() {
        binding.btnAcceptCreate.setOnClickListener {
            submitConsent()
        }
    }

    private fun submitConsent() {
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
                    response.code() == 409 -> {
                        Toast.makeText(
                            this@CreatorConsentActivity,
                            getString(R.string.creator_consent_error_already_exists),
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToMain()
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
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMain()
        }, SUCCESS_REDIRECT_DELAY_MS)
    }

    private fun navigateToMain() {
        val circleId = intent.getStringExtra(EXTRA_CIRCLE_ID).orEmpty()
        val intent = Intent(this, InviteByEmailActivity::class.java).apply {
            putExtra(InviteByEmailActivity.EXTRA_CIRCLE_ID, circleId)
            putExtra(InviteByEmailActivity.EXTRA_CIRCLE_NAME, binding.tvTitle.text?.toString().orEmpty())
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
