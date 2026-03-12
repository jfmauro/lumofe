package be.tivano.lumo.ui.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tokenManager = TokenManager(this)

        lifecycleScope.launch {
            delay(1000L)
            if (tokenManager.isLoggedIn()) {
                navigateTo(MainActivity::class.java)
            } else {
                navigateTo(OnboardingActivity::class.java)
            }
        }
    }

    private fun navigateTo(clazz: Class<*>) {
        startActivity(Intent(this, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
