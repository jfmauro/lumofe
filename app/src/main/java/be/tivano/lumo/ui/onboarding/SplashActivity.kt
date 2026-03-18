package be.tivano.lumo.ui.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient
import be.tivano.lumo.data.TokenManager
import be.tivano.lumo.databinding.ActivitySplashBinding
import be.tivano.lumo.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // KEY CHANGE: installSplashScreen added (DSL / new-ui pattern)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        splashScreen.setKeepOnScreenCondition { false }

        // KEY CHANGE: entrance animations added for logoContainer and taglineContainer
        animateEntrance()

        val tokenManager = TokenManager(this)
        lifecycleScope.launch {
            delay(2000L)
            if (tokenManager.isLoggedIn()) {
                navigateTo(MainActivity::class.java)
            } else {
                navigateTo(OnboardingActivity::class.java)
            }
        }
    }

    private fun animateEntrance() {
        // DSL: slide_up 350ms for logo block
        binding.logoContainer.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)
        )
        // DSL: fade_in delayed 400ms for tagline
        binding.taglineContainer.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed)
        )
    }

    private fun navigateTo(clazz: Class<*>) {
        startActivity(Intent(this, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
