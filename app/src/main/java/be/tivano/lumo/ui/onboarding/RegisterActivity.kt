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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var tokenManager: TokenManager

    private val draftHandler = Handler(Looper.getMainLooper())
    private val draftSaveRunnable = Runnable { saveDraft() }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupValidation()
        setupSubmitButton()
        offerDraftRestoreIfAvailable()
    }

    // ─── REAL-TIME VALIDATION ────────────────────────────────────────────────

    private fun setupValidation() {
        binding.etFirstname.doAfterTextChanged {
            val value = it?.toString().orEmpty()
            if (value.isNotEmpty()) {
                if (ValidationUtil.isValidFirstName(value)) {
                    binding.tilFirstname.error = null
                    binding.tilFirstname.isErrorEnabled = false
                } else {
                    binding.tilFirstname.error = getString(R.string.error_firstname_invalid)
                }
            }
            scheduleDraftSave()
            updateSubmitState()
        }

        binding.etLastname.doAfterTextChanged {
            val value = it?.toString().orEmpty()
            if (value.isNotEmpty()) {
                if (ValidationUtil.isValidLastName(value)) {
                    binding.tilLastname.error = null
                    binding.tilLastname.isErrorEnabled = false
                } else {
                    binding.tilLastname.error = getString(R.string.error_lastname_invalid)
                }
            }
            scheduleDraftSave()
            updateSubmitState()
        }

        binding.etEmail.doAfterTextChanged {
            val value = it?.toString().orEmpty()
            if (value.isNotEmpty()) {
                if (ValidationUtil.isValidEmail(value)) {
                    binding.tilEmail.error = null
                    binding.tilEmail.isErrorEnabled = false
                } else {
                    binding.tilEmail.error = getString(R.string.error_email_invalid)
                }
            }
            scheduleDraftSave()
            updateSubmitState()
        }

        binding.etPhone.doAfterTextChanged {
            val value = it?.toString().orEmpty()
            if (value.isNotEmpty()) {
                if (ValidationUtil.isValidPhone(value)) {
                    binding.tilPhone.error = null
                    binding.tilPhone.isErrorEnabled = false
                } else {
                    binding.tilPhone.error = getString(R.string.error_phone_invalid)
                }
            }
            scheduleDraftSave()
            updateSubmitState()
        }
    }

    private fun updateSubmitState() {
        val firstName = binding.etFirstname.text?.toString().orEmpty()
        val lastName = binding.etLastname.text?.toString().orEmpty()
        val email = binding.etEmail.text?.toString().orEmpty()
        val phone = binding.etPhone.text?.toString().orEmpty()
        binding.btnSubmit.isEnabled = ValidationUtil.isFormValid(firstName, lastName, email, phone)
    }

    // ─── DRAFT AUTO-SAVE ─────────────────────────────────────────────────────

    private fun scheduleDraftSave() {
        draftHandler.removeCallbacks(draftSaveRunnable)
        draftHandler.postDelayed(draftSaveRunnable, 3000L)
    }

    private fun saveDraft() {
        val draft = OnboardingDraft(
            timestamp = System.currentTimeMillis(),
            prenom = binding.etFirstname.text?.toString().orEmpty(),
            nom = binding.etLastname.text?.toString().orEmpty(),
            email = binding.etEmail.text?.toString().orEmpty(),
            telephone = binding.etPhone.text?.toString().orEmpty(),
            disclaimerAccepted = true,
            currentScreen = 4
        )
        DraftManager.saveDraft(this, draft)
    }

    private fun restoreDraft(draft: OnboardingDraft) {
        binding.etFirstname.setText(draft.prenom)
        binding.etLastname.setText(draft.nom)
        binding.etEmail.setText(draft.email)
        binding.etPhone.setText(draft.telephone)
        updateSubmitState()
    }

    private fun offerDraftRestoreIfAvailable() {
        val draft = DraftManager.loadDraft(this)
        if (draft != null && draft.prenom.isNotBlank()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.draft_restore_title)
                .setMessage(R.string.draft_restore_message)
                .setPositiveButton(R.string.draft_restore_yes) { _, _ -> restoreDraft(draft) }
                .setNegativeButton(R.string.draft_restore_no) { _, _ -> DraftManager.clearDraft(this) }
                .show()
        }
    }

    // ─── SUBMIT ──────────────────────────────────────────────────────────────

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener { performRegister() }
    }

    private fun performRegister() {
        val firstName = binding.etFirstname.text?.toString()?.trim().orEmpty()
        val lastName = binding.etLastname.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim()?.ifBlank { null }
        //val disclaimerAccepted =


        if (!ValidationUtil.isFormValid(firstName, lastName, email, phone.orEmpty())) return

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    firstname = firstName,
                    lastname = lastName,
                    email = email,
                    phone = phone,
                    disclaimerAccepted = false,
                    countryCode = "BE"
                )

                val response = RetrofitClient.apiService.register(request)

                when {
                    response.isSuccessful && response.body() != null -> {
                        val body = response.body()!!
                        tokenManager.saveToken(body.token)
                        tokenManager.saveUser(
                            userId = body.userId,
                            firstName = body.prenom,
                            lastName = body.nom,
                            email = body.email
                        )
                        DraftManager.clearDraft(this@RegisterActivity)
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.register_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMain()
                    }
                    response.code() == 409 -> {
                        binding.tilEmail.error = getString(R.string.register_error_409)
                        setLoadingState(false)
                    }
                    response.code() == 400 -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.register_error_400),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 429 -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.register_error_429),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    else -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_LONG
                ).show()
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnSubmit.isEnabled = !loading
        binding.btnSubmit.text = if (loading) {
            getString(R.string.register_loading)
        } else {
            getString(R.string.register_btn_submit)
        }
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        draftHandler.removeCallbacks(draftSaveRunnable)
    }
}
