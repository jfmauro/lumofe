package be.tivano.lumo.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import be.tivano.lumo.R
import be.tivano.lumo.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingPagerAdapter
    private lateinit var stepDots: List<ImageView>

    private var disclaimerAccepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheet) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                resources.getDimensionPixelSize(R.dimen.bottom_sheet_padding_bottom) + navBarInset
            )
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = binding.viewPager.currentItem
                if (current > 0) binding.viewPager.currentItem = current - 1
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        setupViewPager()
        setupStepDots()
        setupButtons()
    }

    private fun setupViewPager() {
        adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { updateUiForStep(position) }
        })
    }

    private fun setupStepDots() {
        // KEY CHANGE: dots are now ImageViews sized to step_dot_size (8dp) for consistency
        // with the DSL ic_step_dot_active (pill) / ic_step_dot_inactive (circle)
        stepDots = List(OnboardingPagerAdapter.TOTAL_STEPS) { index ->
            ImageView(this).apply {
                val size = resources.getDimensionPixelSize(R.dimen.progress_indicator_size)
                val params = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = 6; marginEnd = 6
                }
                layoutParams = params
                setImageResource(
                    if (index == 0) R.drawable.ic_step_dot_active
                    else R.drawable.ic_step_dot_inactive
                )
            }
        }
        stepDots.forEach { binding.stepIndicatorContainer.addView(it) }
    }

    private fun updateUiForStep(step: Int) {
        stepDots.forEachIndexed { index, dot ->
            dot.setImageResource(
                if (index == step) R.drawable.ic_step_dot_active
                else R.drawable.ic_step_dot_inactive
            )
        }

        when (step) {
            OnboardingPagerAdapter.STEP_WELCOME -> {
                binding.btnPrimary.setText(R.string.welcome_btn_create)
                binding.btnSecondary.visibility = View.VISIBLE
                binding.btnPrimary.isEnabled = true
            }
            OnboardingPagerAdapter.STEP_UNDERSTAND -> {
                binding.btnPrimary.setText(R.string.understand_btn_continue)
                binding.btnSecondary.visibility = View.GONE
                binding.btnPrimary.isEnabled = false
                startUnderstandTimer()
            }
            OnboardingPagerAdapter.STEP_DISCLAIMER -> {
                binding.btnPrimary.setText(R.string.disclaimer_btn)
                binding.btnSecondary.visibility = View.GONE
                binding.btnPrimary.isEnabled = disclaimerAccepted
            }
        }
    }

    private fun startUnderstandTimer() {
        val totalSeconds = be.tivano.lumo.BuildConfig.UNDERSTAND_TIMER_SECONDS
        var remaining = totalSeconds
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                remaining--
                if (remaining > 0) {
                    binding.btnPrimary.text = getString(R.string.understand_btn_wait, remaining)
                    handler.postDelayed(this, 1000L)
                } else {
                    binding.btnPrimary.isEnabled = true
                    binding.btnPrimary.setText(R.string.understand_btn_continue)
                }
            }
        }
        handler.postDelayed(runnable, 1000L)
    }

    private fun setupButtons() {
        binding.btnPrimary.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < OnboardingPagerAdapter.TOTAL_STEPS - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                navigateToRegister()
            }
        }
        binding.btnSecondary.setOnClickListener {
            // TODO: navigate to invitation deep-link flow (Feature 0.3)
        }
    }

    fun onDisclaimerCheckChanged(checked: Boolean) {
        disclaimerAccepted = checked
        if (binding.viewPager.currentItem == OnboardingPagerAdapter.STEP_DISCLAIMER) {
            binding.btnPrimary.isEnabled = checked
        }
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}
