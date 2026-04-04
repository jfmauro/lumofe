package be.tivano.lumo.ui.acceptance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityGuestOnboardingBinding
import be.tivano.lumo.model.AcceptInvitationRequest
import be.tivano.lumo.ui.MainActivity
import kotlinx.coroutines.launch

class GuestOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestOnboardingBinding
    private lateinit var tokenManager: TokenManager

    private var currentStep = STEP_ACCOUNT
    private var invitationToken: String = ""
    private var circleName: String = ""
    private var inviterFirstName: String = ""

    companion object {
        const val EXTRA_TOKEN = "extra_invitation_token"
        const val EXTRA_CIRCLE_NAME = "extra_circle_name"
        const val EXTRA_INVITER_FIRST_NAME = "extra_inviter_first_name"
        const val EXTRA_INVITEE_EMAIL = "extra_invitee_email"

        private const val STEP_ACCOUNT = 1
        private const val STEP_CONSENT = 2
        private const val STEP_PREFERENCES = 3

        private const val DEFAULT_CHECKIN_TIME = "20:00"
        private const val DEFAULT_RESPONSE_WINDOW = "8"
        private val PASSWORD_REGEX = Regex("^(?=.*[A-Z])(?=.*\\d).{8,100}$")
        private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    }

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        invitationToken = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()
        inviterFirstName = intent.getStringExtra(EXTRA_INVITER_FIRST_NAME).orEmpty()
        val inviteeEmail = intent.getStringExtra(EXTRA_INVITEE_EMAIL).orEmpty()
        binding.etEmail.setText(inviteeEmail)

        if (invitationToken.isBlank()) {
            finish()
            return
        }

        setupBackButton()
        setupValidation()
        setupNextButton()
        applyStep(STEP_ACCOUNT)
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (currentStep > STEP_ACCOUNT) {
                applyStep(currentStep - 1)
            } else {
                finish()
            }
        }
    }

    private fun setupValidation() {
        binding.etFirstName.doAfterTextChanged { updateNextButtonState() }
        binding.etLastName.doAfterTextChanged { updateNextButtonState() }
        binding.etPassword.doAfterTextChanged { updateNextButtonState() }
        binding.cbConsent.setOnCheckedChangeListener { _, _ -> updateNextButtonState() }
        binding.etCheckinTime.doAfterTextChanged { updateNextButtonState() }
        binding.etResponseWindow.doAfterTextChanged { updateNextButtonState() }
    }

    private fun setupNextButton() {
        binding.btnNext.setOnClickListener {
            when (currentStep) {
                STEP_ACCOUNT -> {
                    if (validateStep1()) applyStep(STEP_CONSENT)
                }
                STEP_CONSENT -> {
                    if (validateStep2()) applyStep(STEP_PREFERENCES)
                }
                STEP_PREFERENCES -> {
                    if (validateStep3()) performAcceptInvitation()
                }
            }
        }
    }

    // ─── STEP NAVIGATION ──────────────────────────────────────────────────────

    private fun applyStep(step: Int) {
        currentStep = step
        hideError()

        binding.layoutStep1.visibility = if (step == STEP_ACCOUNT) View.VISIBLE else View.GONE
        binding.layoutStep2.visibility = if (step == STEP_CONSENT) View.VISIBLE else View.GONE
        binding.layoutStep3.visibility = if (step == STEP_PREFERENCES) View.VISIBLE else View.GONE

        val dotActiveDrawable = be.tivano.lumo.R.drawable.ic_step_dot_active
        val dotInactiveDrawable = be.tivano.lumo.R.drawable.ic_step_dot_inactive

        binding.dotStep1.setBackgroundResource(if (step == STEP_ACCOUNT) dotActiveDrawable else dotInactiveDrawable)
        binding.dotStep2.setBackgroundResource(if (step == STEP_CONSENT) dotActiveDrawable else dotInactiveDrawable)
        binding.dotStep3.setBackgroundResource(if (step == STEP_PREFERENCES) dotActiveDrawable else dotInactiveDrawable)

        when (step) {
            STEP_ACCOUNT -> {
                binding.tvStepLabel.text = getString(R.string.onboarding_guest_step_1_label)
                binding.tvStepTitle.text = getString(R.string.onboarding_guest_step_1_title)
                binding.tvStepSubtitle.text = getString(R.string.onboarding_guest_step_1_subtitle)
                binding.btnNext.text = getString(R.string.common_next)
            }
            STEP_CONSENT -> {
                binding.tvStepLabel.text = getString(R.string.onboarding_guest_step_2_label)
                binding.tvStepTitle.text = getString(R.string.onboarding_guest_step_2_title)
                binding.tvStepSubtitle.text = getString(R.string.onboarding_guest_step_2_subtitle)
                binding.btnNext.text = getString(R.string.common_next)
            }
            STEP_PREFERENCES -> {
                binding.tvStepLabel.text = getString(R.string.onboarding_guest_step_3_label)
                binding.tvStepTitle.text = getString(R.string.onboarding_guest_step_3_title)
                binding.tvStepSubtitle.text = getString(R.string.onboarding_guest_step_3_subtitle)
                binding.btnNext.text = getString(R.string.onboarding_guest_btn_join, circleName)
                if (binding.etCheckinTime.text.isNullOrBlank()) {
                    binding.etCheckinTime.setText(DEFAULT_CHECKIN_TIME)
                }
                if (binding.etResponseWindow.text.isNullOrBlank()) {
                    binding.etResponseWindow.setText(DEFAULT_RESPONSE_WINDOW)
                }
            }
        }

        updateNextButtonState()
    }

    // ─── VALIDATION ───────────────────────────────────────────────────────────

    private fun updateNextButtonState() {
        binding.btnNext.isEnabled = when (currentStep) {
            STEP_ACCOUNT -> {
                val firstName = binding.etFirstName.text?.toString().orEmpty().trim()
                val lastName = binding.etLastName.text?.toString().orEmpty().trim()
                val password = binding.etPassword.text?.toString().orEmpty()
                firstName.length in 1..50 &&
                    lastName.length in 1..50 &&
                    PASSWORD_REGEX.matches(password)
            }
            STEP_CONSENT -> binding.cbConsent.isChecked
            STEP_PREFERENCES -> {
                val time = binding.etCheckinTime.text?.toString().orEmpty().trim()
                val window = binding.etResponseWindow.text?.toString().orEmpty().trim().toIntOrNull()
                (time.isBlank() || TIME_REGEX.matches(time)) &&
                    (window == null || window in 2..24)
            }
            else -> false
        }
    }

    private fun validateStep1(): Boolean {
        val firstName = binding.etFirstName.text?.toString().orEmpty().trim()
        val lastName = binding.etLastName.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (firstName.length !in 1..50) {
            binding.tilFirstName.error = getString(R.string.error_firstname_invalid)
            return false
        }
        binding.tilFirstName.error = null

        if (lastName.length !in 1..50) {
            binding.tilLastName.error = getString(R.string.error_lastname_invalid)
            return false
        }
        binding.tilLastName.error = null

        if (!PASSWORD_REGEX.matches(password)) {
            binding.tilPassword.error = getString(R.string.onboarding_guest_password_error)
            return false
        }
        binding.tilPassword.error = null
        return true
    }

    private fun validateStep2(): Boolean {
        if (!binding.cbConsent.isChecked) {
            showError(getString(R.string.onboarding_guest_consent_required))
            return false
        }
        return true
    }

    private fun validateStep3(): Boolean {
        val timeInput = binding.etCheckinTime.text?.toString().orEmpty().trim()
        if (timeInput.isNotBlank() && !TIME_REGEX.matches(timeInput)) {
            binding.tilCheckinTime.error = getString(R.string.onboarding_guest_checkin_time_error)
            return false
        }
        binding.tilCheckinTime.error = null

        val windowInput = binding.etResponseWindow.text?.toString().orEmpty().trim()
        if (windowInput.isNotBlank()) {
            val window = windowInput.toIntOrNull()
            if (window == null || window !in 2..24) {
                binding.tilResponseWindow.error = getString(R.string.onboarding_guest_response_window_error)
                return false
            }
        }
        binding.tilResponseWindow.error = null
        return true
    }

    // ─── API CALL ─────────────────────────────────────────────────────────────

    private fun performAcceptInvitation() {
        val firstName = binding.etFirstName.text?.toString().orEmpty().trim()
        val lastName = binding.etLastName.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val checkinTime = binding.etCheckinTime.text?.toString().orEmpty().trim()
            .ifBlank { DEFAULT_CHECKIN_TIME }
        val responseWindow = binding.etResponseWindow.text?.toString().orEmpty().trim()
            .toIntOrNull() ?: 8

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.acceptInvitation(
                    token = invitationToken,
                    request = AcceptInvitationRequest(
                        firstName = firstName,
                        lastName = lastName,
                        password = password,
                        consentAccepted = true,
                        preferredCheckinTime = checkinTime,
                        responseWindowHours = responseWindow
                    )
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        val body = response.body()!!
                        tokenManager.saveToken(body.jwtToken)
                        tokenManager.saveUser(
                            userId = body.userId,
                            fullName = "${body.firstName} ${body.lastName}",
                            email = body.email
                        )
                        tokenManager.saveActiveCircle(body.circleId, body.circleName)
                        navigateToMain()
                    }
                    response.code() == 400 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val msg = when {
                            errorBody.contains("CONSENT_REQUIRED") ->
                                getString(R.string.onboarding_guest_consent_required)
                            else ->
                                getString(R.string.register_error_400)
                        }
                        setLoadingState(false)
                        showError(msg)
                    }
                    response.code() == 409 -> {
                        setLoadingState(false)
                        showError(getString(R.string.onboarding_guest_email_already_registered))
                    }
                    response.code() == 410 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val msg = when {
                            errorBody.contains("INVITATION_EXPIRED") ->
                                getString(R.string.landing_error_expired)
                            else ->
                                getString(R.string.landing_error_already_processed)
                        }
                        setLoadingState(false)
                        showError(msg)
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

    // ─── NAVIGATION ───────────────────────────────────────────────────────────

    private fun navigateToMain() {
        Toast.makeText(
            this,
            getString(R.string.onboarding_guest_success, circleName),
            Toast.LENGTH_LONG
        ).show()
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    // ─── LOADING / ERROR STATE ────────────────────────────────────────────────

    private fun setLoadingState(loading: Boolean) {
        binding.btnNext.isEnabled = !loading
        binding.btnNext.text = if (loading) getString(R.string.register_loading)
                               else if (currentStep == STEP_PREFERENCES)
                                   getString(R.string.onboarding_guest_btn_join, circleName)
                               else getString(R.string.common_next)
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
