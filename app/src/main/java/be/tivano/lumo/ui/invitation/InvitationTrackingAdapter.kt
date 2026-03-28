package be.tivano.lumo.ui.invitation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.tivano.lumo.R
import be.tivano.lumo.databinding.ItemInvitationTrackingBinding
import be.tivano.lumo.model.InvitationResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class InvitationTrackingAdapter(
    private val onRevokeClick: (InvitationResponse) -> Unit,
    private val onResendClick: (InvitationResponse) -> Unit
) : ListAdapter<InvitationResponse, InvitationTrackingAdapter.TrackingViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InvitationResponse>() {
            override fun areItemsTheSame(
                oldItem: InvitationResponse,
                newItem: InvitationResponse
            ): Boolean = oldItem.invitationId == newItem.invitationId

            override fun areContentsTheSame(
                oldItem: InvitationResponse,
                newItem: InvitationResponse
            ): Boolean = oldItem == newItem
        }

        private val DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackingViewHolder {
        val binding = ItemInvitationTrackingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackingViewHolder(
        private val binding: ItemInvitationTrackingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InvitationResponse) {
            binding.tvInviteeEmail.text = item.inviteeEmail

            bindSentDate(item)
            bindStatusBadge(item.status)
            bindEmailOpenedIndicator(item)
            bindActions(item)
        }

        // ── Date label — relative ("Sent 3 days ago") ──────────────────────

        private fun bindSentDate(item: InvitationResponse) {
            val ctx = binding.root.context
            val sentAt = item.sentAt
            binding.tvSentAt.text = when {
                sentAt == null -> ctx.getString(R.string.invite_sending)
                else -> {
                    val daysAgo = computeDaysAgo(sentAt)
                    when {
                        daysAgo == null -> ctx.getString(R.string.invite_sent_at, formatDate(sentAt))
                        daysAgo == 0L -> ctx.getString(R.string.tracking_sent_today)
                        daysAgo == 1L -> ctx.getString(R.string.tracking_sent_yesterday)
                        else -> ctx.getString(R.string.tracking_sent_days_ago, daysAgo)
                    }
                }
            }
        }

        // ── Status badge ────────────────────────────────────────────────────

        private fun bindStatusBadge(status: String) {
            val ctx = binding.root.context
            val (labelRes, bgColorRes, textColorRes) = when (status) {
                "PENDING" -> Triple(
                    R.string.invitation_status_pending,
                    R.color.lumo_warning_container,
                    R.color.lumo_warning
                )
                "ACCEPTED" -> Triple(
                    R.string.invitation_status_accepted,
                    R.color.lumo_success_container,
                    R.color.lumo_success
                )
                "DECLINED" -> Triple(
                    R.string.invitation_status_declined,
                    R.color.lumo_secondary_container,
                    R.color.lumo_secondary
                )
                "EXPIRED" -> Triple(
                    R.string.invitation_status_expired,
                    R.color.lumo_surface_variant,
                    R.color.lumo_on_surface_variant
                )
                "REVOKED" -> Triple(
                    R.string.invitation_status_revoked,
                    R.color.lumo_surface_variant,
                    R.color.lumo_on_surface_variant
                )
                else -> Triple(
                    R.string.invitation_status_pending,
                    R.color.lumo_warning_container,
                    R.color.lumo_warning
                )
            }
            binding.tvStatus.text = ctx.getString(labelRes)
            binding.tvStatus.backgroundTintList =
                ContextCompat.getColorStateList(ctx, bgColorRes)
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, textColorRes))
        }

        // ── Email opened indicator ────────────────────────────────────────

        private fun bindEmailOpenedIndicator(item: InvitationResponse) {
            val opened = item.emailOpenedAt != null
            binding.tvEmailOpened.visibility = if (opened) View.VISIBLE else View.GONE
        }

        // ── Contextual action buttons ────────────────────────────────────

        private fun bindActions(item: InvitationResponse) {
            val canRevoke = item.status == "PENDING"
            val canResend = item.status == "PENDING" && item.resendCount < 3
            val canResendExpired = item.status == "EXPIRED"
            val canResendDeclined = item.status == "DECLINED"

            binding.btnRevoke.visibility = if (canRevoke) View.VISIBLE else View.GONE
            binding.btnResend.visibility = if (canResend || canResendExpired || canResendDeclined)
                View.VISIBLE else View.GONE

            if (canRevoke) {
                binding.btnRevoke.setOnClickListener { onRevokeClick(item) }
            }
            if (canResend || canResendExpired || canResendDeclined) {
                binding.btnResend.setOnClickListener { onResendClick(item) }
            }

            // Show resend counter badge when resend > 0 and PENDING
            if (item.resendCount > 0 && item.status == "PENDING") {
                binding.tvResendCount.visibility = View.VISIBLE
                binding.tvResendCount.text =
                    binding.root.context.getString(R.string.tracking_resend_count, item.resendCount)
            } else {
                binding.tvResendCount.visibility = View.GONE
            }
        }

        // ── Helpers ────────────────────────────────────────────────────────

        private fun computeDaysAgo(isoDate: String): Long? {
            return try {
                val sent = ZonedDateTime.parse(isoDate)
                val now = ZonedDateTime.now(sent.zone)
                ChronoUnit.DAYS.between(sent.toLocalDate(), now.toLocalDate())
            } catch (e: Exception) {
                null
            }
        }

        private fun formatDate(isoDate: String): String {
            return try {
                ZonedDateTime.parse(isoDate).format(DISPLAY_FORMATTER)
            } catch (e: Exception) {
                isoDate
            }
        }
    }
}
