package be.tivano.lumo.ui.invitation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.tivano.lumo.R
import be.tivano.lumo.databinding.ItemInvitationBinding
import be.tivano.lumo.model.InvitationResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class InvitationAdapter(
    private val onRevokeClick: (InvitationResponse) -> Unit
) : ListAdapter<InvitationResponse, InvitationAdapter.InvitationViewHolder>(DIFF_CALLBACK) {

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

        private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val binding = ItemInvitationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InvitationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvitationViewHolder(
        private val binding: ItemInvitationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InvitationResponse) {
            binding.tvInviteeEmail.text = item.inviteeEmail

            val sentDate = item.sentAt?.let { parseDisplayDate(it) }
            binding.tvSentAt.text = if (sentDate != null) {
                binding.root.context.getString(R.string.invitation_sent_at, sentDate)
            } else {
                binding.root.context.getString(R.string.invitation_sending)
            }

            applyStatusBadge(item.status)

            val canRevoke = item.status == "PENDING"
            binding.btnRevoke.visibility = if (canRevoke) View.VISIBLE else View.GONE
            if (canRevoke) {
                binding.btnRevoke.setOnClickListener { onRevokeClick(item) }
            }
        }

        private fun applyStatusBadge(status: String) {
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

        private fun parseDisplayDate(isoDate: String): String? {
            return try {
                val zdt = ZonedDateTime.parse(isoDate)
                zdt.format(DISPLAY_FORMATTER)
            } catch (e: Exception) {
                null
            }
        }
    }
}
