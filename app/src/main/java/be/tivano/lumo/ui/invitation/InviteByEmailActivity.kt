package be.tivano.lumo.ui.invitation

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityInviteByEmailBinding
import be.tivano.lumo.model.InvitationRequest
import be.tivano.lumo.model.InvitationResponse
import be.tivano.lumo.ui.MainActivity
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class InviteByEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInviteByEmailBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var invitationAdapter: InvitationAdapter
    private var circleId: String = ""
    private var circleName: String = ""

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"
        const val EXTRA_CIRCLE_NAME = "extra_circle_name"

        private val EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInviteByEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        circleId = intent.getStringExtra(EXTRA_CIRCLE_ID).orEmpty()
        circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()

        if (circleId.isBlank()) {
            Toast.makeText(this, getString(R.string.common_error_generic), Toast.LENGTH_LONG).show()
            navigateToMain()
            return
        }

        setupHeader()
        setupEmailField()
        setupMessageField()
        setupSendButton()
        setupSkipButton()
        setupRecyclerView()
        loadInvitations()
    }

    // ─── HEADER ──────────────────────────────────────────────────────────────

    private fun setupHeader() {
        binding.tvTitle.text = getString(R.string.invite_title, circleName)
    }

    // ─── EMAIL FIELD ─────────────────────────────────────────────────────────

    private fun setupEmailField() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString().orEmpty().trim()
                when {
                    email.isBlank() -> {
                        binding.tilEmail.error = null
                        binding.tilEmail.isErrorEnabled = false
                    }
                    !isValidEmail(email) -> {
                        binding.tilEmail.error = getString(R.string.invite_error_invalid_email)
                    }
                    else -> {
                        binding.tilEmail.error = null
                        binding.tilEmail.isErrorEnabled = false
                    }
                }
                updateSendButtonState()
            }
        })
    }

    // ─── MESSAGE FIELD ───────────────────────────────────────────────────────

    private fun setupMessageField() {
        binding.tvMsgCounter.visibility = View.GONE
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                if (len > 0) {
                    binding.tvMsgCounter.visibility = View.VISIBLE
                    binding.tvMsgCounter.text = getString(R.string.invite_msg_counter, len)
                } else {
                    binding.tvMsgCounter.visibility = View.GONE
                }
                if (len > 200) {
                    binding.tilMessage.error = getString(R.string.invite_error_message_too_long)
                } else {
                    binding.tilMessage.error = null
                    binding.tilMessage.isErrorEnabled = false
                }
                updateSendButtonState()
            }
        })
    }

    // ─── SEND BUTTON ─────────────────────────────────────────────────────────

    private fun setupSendButton() {
        binding.btnSendInvitation.isEnabled = false
        binding.btnSendInvitation.setOnClickListener { performSendInvitation() }
    }

    private fun updateSendButtonState() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val msgLen = binding.etMessage.text?.length ?: 0
        val emailValid = isValidEmail(email) && binding.tilEmail.error == null
        val msgValid = msgLen <= 200
        binding.btnSendInvitation.isEnabled = emailValid && msgValid
    }

    private fun performSendInvitation() {
        val email = binding.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val message = binding.etMessage.text?.toString()?.trim()?.ifBlank { null }

        if (!isValidEmail(email)) return

        setSendingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.sendInvitation(
                    circleId = circleId,
                    request = InvitationRequest(
                        inviteeEmail = email,
                        personalMessage = message,
                        channel = "EMAIL"
                    )
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        onInvitationSuccess(email)
                    }
                    response.code() == 409 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val msgRes = when {
                            errorBody.contains("EMAIL_ALREADY_MEMBER") ->
                                R.string.invite_error_already_member
                            else ->
                                R.string.invite_error_already_invited
                        }
                        binding.tilEmail.error = getString(msgRes)
                        setSendingState(false)
                    }
                    response.code() == 429 -> {
                        Toast.makeText(
                            this@InviteByEmailActivity,
                            getString(R.string.invite_error_limit_reached),
                            Toast.LENGTH_LONG
                        ).show()
                        setSendingState(false)
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@InviteByEmailActivity,
                            getString(R.string.invite_error_forbidden),
                            Toast.LENGTH_LONG
                        ).show()
                        setSendingState(false)
                    }
                    response.code() == 401 -> {
                        Toast.makeText(
                            this@InviteByEmailActivity,
                            getString(R.string.error_session_expired),
                            Toast.LENGTH_LONG
                        ).show()
                        setSendingState(false)
                    }
                    response.code() == 400 -> {
                        Toast.makeText(
                            this@InviteByEmailActivity,
                            getString(R.string.common_error_generic),
                            Toast.LENGTH_LONG
                        ).show()
                        setSendingState(false)
                    }
                    else -> {
                        Toast.makeText(
                            this@InviteByEmailActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_LONG
                        ).show()
                        setSendingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InviteByEmailActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_LONG
                ).show()
                setSendingState(false)
            }
        }
    }

    private fun onInvitationSuccess(email: String) {
        binding.etEmail.text?.clear()
        binding.etMessage.text?.clear()
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false
        binding.tvMsgCounter.visibility = View.GONE
        setSendingState(false)
        Toast.makeText(
            this,
            getString(R.string.invite_success, email),
            Toast.LENGTH_LONG
        ).show()
        loadInvitations()
    }

    // ─── SKIP BUTTON ─────────────────────────────────────────────────────────

    private fun setupSkipButton() {
        binding.btnSkip.setOnClickListener { navigateToMain() }
    }

    // ─── INVITATION LIST ─────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        invitationAdapter = InvitationAdapter { invitation ->
            confirmRevoke(invitation)
        }
        binding.rvInvitations.layoutManager = LinearLayoutManager(this)
        binding.rvInvitations.adapter = invitationAdapter
    }

    private fun loadInvitations() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getInvitations(circleId = circleId)
                if (response.isSuccessful) {
                    val items = response.body()?.content ?: emptyList()
                    invitationAdapter.submitList(items)
                    binding.tvEmptyState.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvInvitations.visibility =
                        if (items.isEmpty()) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                // silent — list is non-critical
            }
        }
    }

    private fun confirmRevoke(invitation: InvitationResponse) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.invite_revoke_title))
            .setMessage(getString(R.string.invite_revoke_message, invitation.inviteeEmail))
            .setPositiveButton(getString(R.string.invite_revoke_confirm)) { _, _ ->
                performRevoke(invitation)
            }
            .setNegativeButton(getString(R.string.invite_revoke_cancel), null)
            .show()
    }

    private fun performRevoke(invitation: InvitationResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.revokeInvitation(
                    circleId = circleId,
                    invitationId = invitation.invitationId
                )
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@InviteByEmailActivity,
                        getString(R.string.invite_revoke_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadInvitations()
                } else {
                    Toast.makeText(
                        this@InviteByEmailActivity,
                        getString(R.string.common_error_server),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InviteByEmailActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && EMAIL_PATTERN.matcher(email).matches()

    private fun setSendingState(loading: Boolean) {
        binding.btnSendInvitation.isEnabled = !loading
        binding.btnSendInvitation.text = if (loading)
            getString(R.string.invite_btn_sending)
        else
            getString(R.string.invite_btn_send)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }
}
