package be.tivano.lumo.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityRegisterBinding
import be.tivano.lumo.model.OnboardingDraft
import be.tivano.lumo.model.RegisterRequest
import be.tivano.lumo.ui.MainActivity
import be.tivano.lumo.util.DraftManager
import be.tivano.lumo.util.ValidationUtil
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var tokenManager: TokenManager

    private val draftHandler = Handler(Looper.getMainLooper())
    private val draftSaveRunnable = Runnable { saveDraft() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        setupValidation()
        setupSubmitButton()
        offerDraftRestoreIfAvailable()
    }

    // ─── VALIDATION ──────────────────────────────────────────────────────────
    // KEY CHANGES: binding IDs updated to match activity_register.xml DSL layout
    //   tilFirstname  -> tilFirstName
    //   etFirstname   -> etFirstName
    //   tilLastname   -> tilLastName
    //   etLastname    -> etLastName
    //   btnSubmit     -> btnRegister
    //   Added: etPassword / tilPassword validation

    private fun setupValidation() {
        binding.etFirstName.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            if (v.isNotEmpty()) {
                if (ValidationUtil.isValidFirstName(v)) {
                    binding.tilFirstName.error = null
                    binding.tilFirstName.isErrorEnabled = false
                } else {
                    binding.tilFirstName.error = getString(R.string.error_firstname_invalid)
                }
            }
            scheduleDraftSave(); updateSubmitState()
        }

        binding.etLastName.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            if (v.isNotEmpty()) {
                if (ValidationUtil.isValidLastName(v)) {
                    binding.tilLastName.error = null
                    binding.tilLastName.isErrorEnabled = false
                } else {
                    binding.tilLastName.error = getString(R.string.error_lastname_invalid)
                }
            }
            scheduleDraftSave(); updateSubmitState()
        }

        binding.etEmail.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            if (v.isNotEmpty()) {
                if (ValidationUtil.isValidEmail(v)) {
                    binding.tilEmail.error = null
                    binding.tilEmail.isErrorEnabled = false
                } else {
                    binding.tilEmail.error = getString(R.string.error_email_invalid)
                }
            }
            scheduleDraftSave(); updateSubmitState()
        }

        binding.etPhone.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            if (v.isNotEmpty()) {
                if (ValidationUtil.isValidPhone(v)) {
                    binding.tilPhone.error = null
                    binding.tilPhone.isErrorEnabled = false
                } else {
                    binding.tilPhone.error = getString(R.string.error_phone_invalid)
                }
            }
            scheduleDraftSave(); updateSubmitState()
        }

        binding.etPassword.doAfterTextChanged {
            updateSubmitState()
        }
    }

    private fun updateSubmitState() {
        val firstName = binding.etFirstName.text?.toString().orEmpty()
        val lastName  = binding.etLastName.text?.toString().orEmpty()
        val email     = binding.etEmail.text?.toString().orEmpty()
        val phone     = binding.etPhone.text?.toString().orEmpty()
        val password  = binding.etPassword.text?.toString().orEmpty()
        binding.btnRegister.isEnabled =
            ValidationUtil.isFormValid(firstName, lastName, email, phone) && password.length >= 8
    }

    // ─── DRAFT AUTO-SAVE ─────────────────────────────────────────────────────

    private fun scheduleDraftSave() {
        draftHandler.removeCallbacks(draftSaveRunnable)
        draftHandler.postDelayed(draftSaveRunnable, 3000L)
    }

    private fun saveDraft() {
        DraftManager.saveDraft(this, OnboardingDraft(
            timestamp       = System.currentTimeMillis(),
            firstname       = binding.etFirstName.text?.toString().orEmpty(),
            lastname        = binding.etLastName.text?.toString().orEmpty(),
            email           = binding.etEmail.text?.toString().orEmpty(),
            phone           = binding.etPhone.text?.toString().orEmpty(),
            disclaimerAccepted = true,
            currentScreen   = 4
        ))
    }

    private fun offerDraftRestoreIfAvailable() {
        val draft = DraftManager.loadDraft(this) ?: return
        if (draft.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.draft_restore_title)
                .setMessage(R.string.draft_restore_message)
                .setPositiveButton(R.string.draft_restore_yes) { _, _ -> restoreDraft(draft) }
                .setNegativeButton(R.string.draft_restore_no)  { _, _ -> DraftManager.clearDraft(this) }
                .show()
        }
    }

    private fun restoreDraft(draft: OnboardingDraft) {
        val safe = draft.sanitized()
        binding.etFirstName.setText(safe.firstname)
        binding.etLastName.setText(safe.lastname)
        binding.etEmail.setText(safe.email)
        binding.etPhone.setText(safe.phone)
        updateSubmitState()
    }

    // ─── SUBMIT ──────────────────────────────────────────────────────────────

    private fun setupSubmitButton() {
        binding.btnRegister.setOnClickListener { performRegister() }
    }

    private fun performRegister() {
        val firstName = binding.etFirstName.text?.toString()?.trim().orEmpty()
        val lastName  = binding.etLastName.text?.toString()?.trim().orEmpty()
        val email     = binding.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val phone     = binding.etPhone.text?.toString()?.trim()?.ifBlank { null }
        val password  = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (!ValidationUtil.isFormValid(firstName, lastName, email, phone.orEmpty())) return
        if (password.length < 8) return

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(
                        firstname          = firstName,
                        lastname           = lastName,
                        email              = email,
                        phone              = phone,
                        countryCode        = "BE",
                        disclaimerAccepted = true
                    )
                )

                when {
                    response.isSuccessful && response.body() != null -> {
                        val body = response.body()!!
                        tokenManager.saveToken(body.token)
                        tokenManager.saveUser(
                            userId   = body.user.userId,
                            fullName = body.user.fullName,
                            email    = body.user.email
                        )
                        DraftManager.clearDraft(this@RegisterActivity)
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    response.code() == 409 -> {
                        binding.tilEmail.error = getString(R.string.register_error_409)
                        setLoadingState(false)
                    }
                    response.code() == 400 -> {
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.register_error_400), Toast.LENGTH_LONG).show()
                        setLoadingState(false)
                    }
                    response.code() == 429 -> {
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.register_error_429), Toast.LENGTH_LONG).show()
                        setLoadingState(false)
                    }
                    else -> {
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.common_error_server), Toast.LENGTH_LONG).show()
                        setLoadingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity,
                    getString(R.string.common_error_network), Toast.LENGTH_LONG).show()
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) getString(R.string.register_loading)
                                   else         getString(R.string.register_btn_submit)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }


    private fun navigateToMain() {
        startActivity(Intent(this, be.tivano.lumo.ui.circle.CreateCircleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        draftHandler.removeCallbacks(draftSaveRunnable)
    }
}
