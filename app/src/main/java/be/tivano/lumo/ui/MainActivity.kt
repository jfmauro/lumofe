package be.tivano.lumo.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivityMainBinding
import be.tivano.lumo.ui.invitation.InvitationTrackingActivity
import be.tivano.lumo.ui.onboarding.OnboardingActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)


        setupNavigationView()
        setupBottomNavBar()

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // ─── BOTTOM NAV BAR ──────────────────────────────────────────────────────

    private fun setupBottomNavBar() {
        binding.btnNavMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnNavHome.setOnClickListener {
            // Reserved for Epic 1 home / check-in fragment
        }

        binding.btnNavInvitations.setOnClickListener {
            navigateToInvitationTracking()
        }
    }

    /**
     * Shows the bottom nav bar only when logged in.
     * The invitations button is additionally gated on the CREATOR role.
     */
    private fun updateBottomNavVisibility() {
        lifecycleScope.launch {
            val isLoggedIn = tokenManager.isLoggedIn()
            binding.bottomNavBar.visibility = if (isLoggedIn) View.VISIBLE else View.GONE

            if (isLoggedIn) {
                val isCreator = tokenManager.isCircleCreator()
                binding.btnNavInvitations.visibility =
                    if (isCreator) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateNavHeaderUserName() {
        lifecycleScope.launch {
            val firstName = tokenManager.getUserFirstName().orEmpty()
            val headerView = binding.navigationView.getHeaderView(0)
            val tvUserName = headerView.findViewById<android.widget.TextView>(R.id.navHeaderUserName)
            tvUserName?.text = if (firstName.isNotBlank()) firstName else ""
        }
    }

    /**
     * Shows the invitations item in the navigation drawer only for circle creators.
     * Called every onResume to reflect the current role without requiring a restart.
     */
    private fun updateDrawerInvitationsVisibility() {
        lifecycleScope.launch {
            val isCreator = tokenManager.isCircleCreator()
            binding.navigationView.menu
                .findItem(R.id.nav_invitations)
                ?.isVisible = isCreator
        }
    }

    // ─── NAVIGATION VIEW ─────────────────────────────────────────────────────

    private fun setupNavigationView() {
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> { /* Reserved */ }
            R.id.nav_invitations -> navigateToInvitationTracking()
            R.id.nav_profile -> { /* Reserved for profile screen */ }
            R.id.nav_logout -> performLogout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // ─── INVITATION TRACKING ─────────────────────────────────────────────────

    private fun navigateToInvitationTracking() {
        lifecycleScope.launch {
            // Double-check: a non-creator should never reach this, but we guard anyway
            if (!tokenManager.isCircleCreator()) return@launch

            val circleId = tokenManager.getActiveCircleId()
            val circleName = tokenManager.getActiveCircleName()
            if (circleId.isNullOrBlank()) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.main_no_active_circle),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val intent = Intent(this@MainActivity, InvitationTrackingActivity::class.java).apply {
                putExtra(InvitationTrackingActivity.EXTRA_CIRCLE_ID, circleId)
                putExtra(InvitationTrackingActivity.EXTRA_CIRCLE_NAME, circleName.orEmpty())
            }
            startActivity(intent)
        }
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    private fun performLogout() {
        lifecycleScope.launch {
            tokenManager.clearAll()
            navigateToOnboarding()
        }
    }

    private fun navigateToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // ─── LIFECYCLE ───────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        updateBottomNavVisibility()
        updateNavHeaderUserName()
        updateDrawerInvitationsVisibility()
    }

}