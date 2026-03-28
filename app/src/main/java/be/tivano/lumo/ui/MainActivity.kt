package be.tivano.lumo.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
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
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupToolbar()
        setupDrawer()
        setupNavigationView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.nav_drawer_opened,
            R.string.nav_drawer_opened
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(R.color.lumo_primary)
    }

    private fun setupNavigationView() {
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_invitations -> navigateToInvitationTracking()
            R.id.nav_logout -> performLogout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // ─── INVITATION TRACKING ─────────────────────────────────────────────────

    private fun navigateToInvitationTracking() {
        lifecycleScope.launch {
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
