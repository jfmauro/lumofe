package be.tivano.lumo.ui.circle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.databinding.ActivityCircleSettingsBinding
import be.tivano.lumo.model.CreateCircleSettingsRequest
import kotlinx.coroutines.launch

class CircleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCircleSettingsBinding

    // ─── SELECTION STATE ─────────────────────────────────────────────────────

    private var selectedTargetSize: String = TARGET_SIZE_MEDIUM
    private var selectedCheckinMode: String = CHECKIN_MODE_SIMPLE
    private var selectedProtectionLevel: String = PROTECTION_STANDARD

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"

        const val EXTRA_CIRCLE_NAME = "extra_circle_name"

        private const val TARGET_SIZE_SMALL = "SMALL"
        private const val TARGET_SIZE_MEDIUM = "MEDIUM"
        private const val TARGET_SIZE_LARGE = "LARGE"

        private const val CHECKIN_MODE_SIMPLE = "SIMPLE"
        private const val CHECKIN_MODE_WITH_ENERGY = "WITH_ENERGY"

        private const val PROTECTION_STANDARD = "STANDARD"
        private const val PROTECTION_REINFORCED = "REINFORCED"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCircleSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupSizeOptions()
        setupCheckinOptions()
        setupProtectionOptions()
        setupFinalizeButton()
        refreshSummaryChips()
    }

    // ─── BACK BUTTON ─────────────────────────────────────────────────────────

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    // ─── SECTION 1 — TARGET SIZE ─────────────────────────────────────────────

    private fun setupSizeOptions() {
        selectSizeCard(TARGET_SIZE_MEDIUM)

        binding.cardSizeSmall.setOnClickListener {
            selectSizeCard(TARGET_SIZE_SMALL)
        }
        binding.cardSizeMedium.setOnClickListener {
            selectSizeCard(TARGET_SIZE_MEDIUM)
        }
        binding.cardSizeLarge.setOnClickListener {
            selectSizeCard(TARGET_SIZE_LARGE)
        }
    }

    private fun selectSizeCard(size: String) {
        selectedTargetSize = size
        applyCardSelection(
            selectedKey = size,
            options = mapOf(
                TARGET_SIZE_SMALL to Pair(binding.cardSizeSmall, binding.radioDotSizeSmall),
                TARGET_SIZE_MEDIUM to Pair(binding.cardSizeMedium, binding.radioDotSizeMedium),
                TARGET_SIZE_LARGE to Pair(binding.cardSizeLarge, binding.radioDotSizeLarge)
            )
        )
        refreshSummaryChips()
    }

    // ─── SECTION 2 — CHECK-IN MODE ───────────────────────────────────────────

    private fun setupCheckinOptions() {
        selectCheckinCard(CHECKIN_MODE_SIMPLE)

        binding.cardCheckinSimple.setOnClickListener {
            selectCheckinCard(CHECKIN_MODE_SIMPLE)
        }
        binding.cardCheckinEnergy.setOnClickListener {
            selectCheckinCard(CHECKIN_MODE_WITH_ENERGY)
        }
    }

    private fun selectCheckinCard(mode: String) {
        selectedCheckinMode = mode
        applyCardSelection(
            selectedKey = mode,
            options = mapOf(
                CHECKIN_MODE_SIMPLE to Pair(binding.cardCheckinSimple, binding.radioDotCheckinSimple),
                CHECKIN_MODE_WITH_ENERGY to Pair(binding.cardCheckinEnergy, binding.radioDotCheckinEnergy)
            )
        )
        refreshSummaryChips()
    }

    // ─── SECTION 3 — PROTECTION LEVEL ────────────────────────────────────────

    private fun setupProtectionOptions() {
        selectProtectionCard(PROTECTION_STANDARD)

        binding.cardProtectionStandard.setOnClickListener {
            selectProtectionCard(PROTECTION_STANDARD)
        }
        binding.cardProtectionReinforced.setOnClickListener {
            selectProtectionCard(PROTECTION_REINFORCED)
        }
    }

    private fun selectProtectionCard(level: String) {
        selectedProtectionLevel = level
        applyCardSelection(
            selectedKey = level,
            options = mapOf(
                PROTECTION_STANDARD to Pair(binding.cardProtectionStandard, binding.radioDotProtectionStandard),
                PROTECTION_REINFORCED to Pair(binding.cardProtectionReinforced, binding.radioDotProtectionReinforced)
            )
        )
        // Show / hide timeline inside REINFORCED card
        binding.timelineReinforced.visibility =
            if (level == PROTECTION_REINFORCED) View.VISIBLE else View.GONE
        refreshSummaryChips()
    }

    // ─── GENERIC CARD SELECTION HELPER ───────────────────────────────────────

    private fun applyCardSelection(
        selectedKey: String,
        options: Map<String, Pair<com.google.android.material.card.MaterialCardView, View>>
    ) {
        options.forEach { (key, pair) ->
            val card = pair.first
            val dot = pair.second
            val isSelected = key == selectedKey
            if (isSelected) {
                card.setCardBackgroundColor(getColor(R.color.lumo_primary_container))
                card.strokeColor = getColor(R.color.lumo_primary)
                dot.setBackgroundResource(R.drawable.bg_timeline_dot_active)
            } else {
                card.setCardBackgroundColor(getColor(R.color.white))
                card.strokeColor = getColor(R.color.lumo_outline_variant)
                dot.setBackgroundResource(R.drawable.bg_timeline_dot_inactive)
            }
        }
    }

    // ─── SUMMARY CHIPS ───────────────────────────────────────────────────────

    private fun refreshSummaryChips() {
        binding.chipSummarySize.text = when (selectedTargetSize) {
            TARGET_SIZE_SMALL -> getString(R.string.circle_settings_chip_small)
            TARGET_SIZE_LARGE -> getString(R.string.circle_settings_chip_large)
            else -> getString(R.string.circle_settings_chip_medium)
        }
        binding.chipSummaryCheckin.text = when (selectedCheckinMode) {
            CHECKIN_MODE_WITH_ENERGY -> getString(R.string.circle_settings_chip_energy)
            else -> getString(R.string.circle_settings_chip_simple)
        }
        binding.chipSummaryProtection.text = when (selectedProtectionLevel) {
            PROTECTION_REINFORCED -> getString(R.string.circle_settings_chip_reinforced)
            else -> getString(R.string.circle_settings_chip_standard)
        }
    }

    // ─── FINALIZE BUTTON ─────────────────────────────────────────────────────

    private fun setupFinalizeButton() {
        binding.btnFinalize.setOnClickListener { performSaveSettings() }
    }

    private fun performSaveSettings() {
        val circleId = intent.getStringExtra(EXTRA_CIRCLE_ID)
        if (circleId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.common_error_generic), Toast.LENGTH_LONG).show()
            return
        }

        val metricsVisible = binding.switchMetrics.isChecked

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createCircleSettings(
                    circleId = circleId,
                    request = CreateCircleSettingsRequest(
                        targetSize = selectedTargetSize,
                        checkinMode = selectedCheckinMode,
                        protectionLevel = selectedProtectionLevel,
                        metricsVisible = metricsVisible
                    )
                )
                when {
                    response.isSuccessful && response.body() != null -> {
                        navigateToCreatorConsent(circleId)
                    }
                    response.code() == 401 -> {
                        Toast.makeText(
                            this@CircleSettingsActivity,
                            getString(R.string.error_session_expired),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@CircleSettingsActivity,
                            getString(R.string.circle_settings_error_forbidden),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 404 -> {
                        Toast.makeText(
                            this@CircleSettingsActivity,
                            getString(R.string.circle_settings_error_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    response.code() == 409 -> {
                        Toast.makeText(
                            this@CircleSettingsActivity,
                            getString(R.string.circle_settings_error_already_exists),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                    else -> {
                        Toast.makeText(
                            this@CircleSettingsActivity,
                            getString(R.string.common_error_server),
                            Toast.LENGTH_LONG
                        ).show()
                        setLoadingState(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CircleSettingsActivity,
                    getString(R.string.common_error_network),
                    Toast.LENGTH_LONG
                ).show()
                setLoadingState(false)
            }
        }
    }

    private fun navigateToCreatorConsent(circleId: String) {
        val circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME).orEmpty()
        val intent = Intent(this, CreatorConsentActivity::class.java).apply {
            putExtra(CreatorConsentActivity.EXTRA_CIRCLE_ID, circleId)
            putExtra(CreatorConsentActivity.EXTRA_CIRCLE_NAME, circleName)
        }
        startActivity(intent)
        finish()
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnFinalize.isEnabled = !loading
        binding.btnFinalize.text = if (loading)
            getString(R.string.circle_settings_btn_saving)
        else
            getString(R.string.circle_settings_btn_finalize)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
