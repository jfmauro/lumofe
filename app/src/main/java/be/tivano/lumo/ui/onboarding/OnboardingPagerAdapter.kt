package be.tivano.lumo.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val STEP_WELCOME = 0
        const val STEP_UNDERSTAND = 1
        const val STEP_DISCLAIMER = 2
        const val TOTAL_STEPS = 3
    }

    override fun getItemCount(): Int = TOTAL_STEPS

    override fun createFragment(position: Int): Fragment = when (position) {
        STEP_WELCOME -> WelcomeFragment()
        STEP_UNDERSTAND -> UnderstandFragment()
        STEP_DISCLAIMER -> DisclaimerFragment()
        else -> WelcomeFragment()
    }
}
