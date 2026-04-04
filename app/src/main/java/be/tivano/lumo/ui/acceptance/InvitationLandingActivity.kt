package be.tivano.lumo.ui.acceptance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.databinding.ActivityInvitationLandingBinding
import be.tivano.lumo.model.InvitationLandingResponse
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class InvitationLandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvitationLandingBinding

    private var invitationToken: String? = null
    private var landingData: InvitationLandingResponse? = null

    companion object {
        const val EXTRA_TOKEN = "extra_invitation_token"
        private const val TOKEN_REGEX = "^[a-f0-9]{64}$"
    }

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()

        val tokenFromExtra = intent.getStringExtra(EXTRA_TOKEN)
        val tokenFromDeepLink = extractTokenFromDeepLink(intent)
        val token = tokenFromExtra ?: tokenFromDeepLink

        if (token != null) {
            invitationToken = token
            showLoadingState()
            loadLandingData(token)
        } else {
            showTokenInputState()
            setupTokenInput()
        }

        setupActionButtons()
    }

    // ─── DEEP LINK HANDLING ──────────────────────────────────────────────────

    private fun extractTokenFromDeepLink(intent: Intent): String? {
        val uri: Uri = intent.data ?: return null
        val segments = uri.pathSegments
        return if (segments.size >= 2 && segments[segments.size - 2] == "join") {
            segments.last()
        } else {
            null
        }
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTokenInput() {
        binding.etToken.doAfterTextChanged { text ->
            val value = text?.toString().orEmpty().trim()
            binding.btnVerifyToken.isEnabled = value.matches(Regex(TOKEN_REGEX)) ||
                value.contains("/join/")
        }

        binding.btnVerifyToken.setOnClickListener {
            val rawInput = binding.etToken.text?.toString().orEmpty().trim()
            val token = extractTokenFromString(rawInput)
            if (token == null) {
                binding.tilToken.error = getString(R.string.landing_error_invalid_token_format)
                return@setOnClickListener
            }
            invitationToken = token
            binding.tilToken.error = null
            showLoadingState()
            loadLandingData(token)
        }
    }

    private fun setupActionButtons() {
        binding.btnAccept.setOnClickListener {
            val data = landingData ?: return@setOnClickListener
            val intent = Intent(this, GuestOnboardingActivity::class.java).apply {
                putExtra(GuestOnboardingActivity.EXTRA_TOKEN, data.invitationToken)
                putExtra(GuestOnboardingActivity.EXTRA_CIRCLE_NAME, data.circleName)
                putExtra(GuestOnboardingActivity.EXTRA_INVITER_FIRST_NAME, data.inviterFirstName)
            }
            startActivity(intent)
        }

        binding.btnDecline.setOnClickListener {
            val data = landingData ?: return@setOnClickListener
            val intent = Intent(this, InvitationDeclineActivity::class.java).apply {
                putExtra(InvitationDeclineActivity.EXTRA_TOKEN, data.invitationToken)
                putExtra(InvitationDeclineActivity.EXTRA_CIRCLE_NAME, data.circleName)
                putExtra(InvitationDeclineActivity.EXTRA_INVITER_FIRST_NAME, data.inviterFirstName)
            }
            startActivity(intent)
        }
    }

    // ─── DATA LOADING ─────────────────────────────────────────────────────────

    private fun loadLandingData(token: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getInvitationLanding(token)
                when {
                    response.isSuccessful && response.body() != null -> {
                        landingData = response.body()!!
                        showLandingState(response.body()!!)
                    }
                    response.code() == 400 -> {
                        showErrorState(
                            title = getString(R.string.landing_error_400_title),
                            message = getString(R.string.landing_error_invalid_token_format)
                        )
                    }
                    response.code() == 404 -> {
                        showErrorState(
                            title = getString(R.string.landing_error_404_title),
                            message = getString(R.string.landing_error_not_found)
                        )
                    }
                    response.code() == 410 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val message = when {
                            errorBody.contains("INVITATION_EXPIRED") ->
                                getString(R.string.landing_error_expired)
                            errorBody.contains("INVITATION_ALREADY_PROCESSED") ->
                                getString(R.string.landing_error_already_processed)
                            else ->
                                getString(R.string.landing_error_expired)
                        }
                        showErrorState(
                            title = getString(R.string.landing_error_410_title),
                            message = message
                        )
                    }
                    else -> {
                        showErrorState(
                            title = getString(R.string.common_error_server),
                            message = getString(R.string.common_error_generic)
                        )
                    }
                }
            } catch (e: Exception) {
                showErrorState(
                    title = getString(R.string.common_error_network),
                    message = getString(R.string.common_error_network)
                )
            }
        }
    }

    // ─── UI STATE MANAGEMENT ──────────────────────────────────────────────────

    private fun showTokenInputState() {
        binding.cardTokenInput.visibility = View.VISIBLE
        binding.progressLoading.visibility = View.GONE
        binding.cardError.visibility = View.GONE
        binding.cardLanding.visibility = View.GONE
        binding.btnAccept.visibility = View.GONE
        binding.btnDecline.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.cardTokenInput.visibility = View.GONE
        binding.progressLoading.visibility = View.VISIBLE
        binding.cardError.visibility = View.GONE
        binding.cardLanding.visibility = View.GONE
        binding.btnAccept.visibility = View.GONE
        binding.btnDecline.visibility = View.GONE
    }

    private fun showLandingState(data: InvitationLandingResponse) {
        binding.progressLoading.visibility = View.GONE
        binding.cardTokenInput.visibility = View.GONE
        binding.cardError.visibility = View.GONE

        binding.tvInviterGreeting.text = getString(
            R.string.landing_inviter_greeting,
            data.inviterFirstName,
            data.inviterLastName,
            data.circleName
        )
        binding.tvCircleName.text = data.circleName
        binding.tvMemberCount.text = getString(R.string.landing_member_count, data.currentMemberCount)
        binding.tvExpiresAt.text = formatExpirationDate(data.expiresAt)

        binding.cardLanding.visibility = View.VISIBLE
        binding.btnAccept.visibility = View.VISIBLE
        binding.btnDecline.visibility = View.VISIBLE
    }

    private fun showErrorState(title: String, message: String) {
        binding.progressLoading.visibility = View.GONE
        binding.cardLanding.visibility = View.GONE
        binding.btnAccept.visibility = View.GONE
        binding.btnDecline.visibility = View.GONE

        binding.tvErrorTitle.text = title
        binding.tvErrorMessage.text = message
        binding.cardError.visibility = View.VISIBLE

        if (invitationToken == null) {
            binding.cardTokenInput.visibility = View.VISIBLE
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private fun extractTokenFromString(input: String): String? {
        if (input.matches(Regex(TOKEN_REGEX))) return input
        val joinPattern = Regex("/join/([a-f0-9]{64})")
        val match = joinPattern.find(input)
        return match?.groupValues?.getOrNull(1)
    }

    private fun formatExpirationDate(iso: String): String {
        return try {
            val instant = Instant.parse(iso)
            val formatter = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            iso
        }
    }
}
