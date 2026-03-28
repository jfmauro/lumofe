package be.tivano.lumo.ui.invitation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.databinding.ActivityInvitationTrackingBinding
import be.tivano.lumo.model.InvitationResponse
import be.tivano.lumo.model.InvitationStatisticsResponse
import kotlinx.coroutines.launch

class InvitationTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvitationTrackingBinding
    private lateinit var trackingAdapter: InvitationTrackingAdapter

    private var circleId: String = ""
    private var circleName: String = ""
    private var currentFilter: String? = null

    private val pollingHandler = Handler(Looper.getMainLooper())
    private val pollingRunnable = object : Runnable {
        override fun run() {
            loadInvitations()
            loadStatistics()
            pollingHandler.postDelayed(this, POLLING_INTERVAL_MS)
        }
    }

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"
        const val EXTRA_CIRCLE_NAME = "extra_circle_name"
        private const val POLLING_INTERVAL_MS = 30_000L
    }

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        circleId = intent.getStringExtra(EXTRA_CIRCLE_ID).orEmpty()
        circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()

        if (circleId.isBlank()) {
            Toast.makeText(this, getString(R.string.common_error_generic), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupHeader()
        setupBackButton()
        setupFilterTabs()
        setupRecyclerView()
        setupSendInvitationButton()
        initialLoad()
    }

    override fun onResume() {
        super.onResume()
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        pollingHandler.removeCallbacks(pollingRunnable)
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    private fun setupHeader() {
        binding.tvTitle.text = getString(R.string.tracking_title, circleName)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        trackingAdapter = InvitationTrackingAdapter(
            onRevokeClick = { invitation -> confirmRevoke(invitation) },
            onResendClick = { invitation -> confirmResend(invitation) }
        )
        binding.rvInvitations.layoutManager = LinearLayoutManager(this)
        binding.rvInvitations.adapter = trackingAdapter
        binding.rvInvitations.itemAnimator = null
    }

    private fun setupSendInvitationButton() {
        binding.btnSendInvitation.setOnClickListener {
            val intent = Intent(this, InviteByEmailActivity::class.java).apply {
                putExtra(InviteByEmailActivity.EXTRA_CIRCLE_ID, circleId)
                putExtra(InviteByEmailActivity.EXTRA_CIRCLE_NAME, circleName)
            }
            startActivity(intent)
        }
    }

    // ─── FILTER TABS ─────────────────────────────────────────────────────────

    private fun setupFilterTabs() {
        setFilterActive(binding.tabAll)
        binding.tabAll.setOnClickListener { applyFilter(null, binding.tabAll) }
        binding.tabPending.setOnClickListener { applyFilter("PENDING", binding.tabPending) }
        binding.tabAccepted.setOnClickListener { applyFilter("ACCEPTED", binding.tabAccepted) }
        binding.tabDeclined.setOnClickListener { applyFilter("DECLINED", binding.tabDeclined) }
        binding.tabExpired.setOnClickListener { applyFilter("EXPIRED", binding.tabExpired) }
        binding.tabRevoked.setOnClickListener { applyFilter("REVOKED", binding.tabRevoked) }
    }

    private fun applyFilter(status: String?, tab: android.widget.TextView) {
        currentFilter = status
        setFilterActive(tab)
        loadInvitations()
    }

    private fun setFilterActive(activeTab: android.widget.TextView) {
        val allTabs = listOf(
            binding.tabAll, binding.tabPending, binding.tabAccepted,
            binding.tabDeclined, binding.tabExpired, binding.tabRevoked
        )
        allTabs.forEach { tab ->
            tab.isSelected = (tab == activeTab)
            tab.setTextColor(
                if (tab == activeTab)
                    ContextCompat.getColor(this, R.color.white)
                else
                    ContextCompat.getColor(this, R.color.lumo_on_surface_muted)
            )
            tab.backgroundTintList = if (tab == activeTab)
                ContextCompat.getColorStateList(this, R.color.lumo_primary)
            else
                ContextCompat.getColorStateList(this, R.color.lumo_surface_variant)
        }
    }

    // ─── DATA LOADING ─────────────────────────────────────────────────────────

    private fun initialLoad() {
        binding.progressInitial.visibility = View.VISIBLE
        binding.rvInvitations.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        loadInvitations()
        loadStatistics()
    }

    private fun loadInvitations() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getInvitations(
                    circleId = circleId,
                    status = currentFilter,
                    page = 0,
                    size = 50,
                    sort = "sentAt,desc"
                )
                binding.progressInitial.visibility = View.GONE
                if (response.isSuccessful) {
                    val items = response.body()?.content ?: emptyList()
                    trackingAdapter.submitList(items)
                    updateEmptyState(items.isEmpty())
                } else if (response.code() == 401) {
                    Toast.makeText(
                        this@InvitationTrackingActivity,
                        getString(R.string.error_session_expired),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressInitial.visibility = View.GONE
                Toast.makeText(
                    this@InvitationTrackingActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getInvitationStatistics(circleId = circleId)
                if (response.isSuccessful) {
                    response.body()?.let { updateStatsBlock(it) }
                }
            } catch (e: Exception) {
                // Stats block stays hidden — non-blocking
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvInvitations.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateStatsBlock(stats: InvitationStatisticsResponse) {
        binding.statsCard.visibility = View.VISIBLE
        binding.tvStatSent.text = getString(R.string.tracking_stat_sent, stats.totalSent)
        binding.tvStatAccepted.text = getString(
            R.string.tracking_stat_accepted,
            stats.totalAccepted,
            stats.acceptanceRate.toInt()
        )
        binding.tvStatPending.text = getString(R.string.tracking_stat_pending, stats.totalPending)
        binding.tvStatMembers.text = getString(
            R.string.tracking_stat_members,
            stats.currentMemberCount
        )
    }

    // ─── REVOKE ───────────────────────────────────────────────────────────────

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
                when {
                    response.isSuccessful -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.invite_revoke_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        loadInvitations()
                        loadStatistics()
                    }
                    response.code() == 400 -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.tracking_error_revoke_invalid_state),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.invite_error_forbidden),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InvitationTrackingActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ─── RESEND ───────────────────────────────────────────────────────────────

    private fun confirmResend(invitation: InvitationResponse) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tracking_resend_title))
            .setMessage(getString(R.string.tracking_resend_message, invitation.inviteeEmail))
            .setPositiveButton(getString(R.string.tracking_resend_confirm)) { _, _ ->
                performResend(invitation)
            }
            .setNegativeButton(getString(R.string.invite_revoke_cancel), null)
            .show()
    }

    private fun performResend(invitation: InvitationResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resendInvitation(
                    circleId = circleId,
                    invitationId = invitation.invitationId
                )
                when {
                    response.isSuccessful -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.tracking_resend_success, invitation.inviteeEmail),
                            Toast.LENGTH_SHORT
                        ).show()
                        loadInvitations()
                    }
                    response.code() == 400 -> {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val msgRes = when {
                            errorBody.contains("RESEND_LIMIT_REACHED") ->
                                R.string.tracking_error_resend_limit
                            else ->
                                R.string.tracking_error_resend_invalid_state
                        }
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(msgRes),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.invite_error_forbidden),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this@InvitationTrackingActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InvitationTrackingActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
