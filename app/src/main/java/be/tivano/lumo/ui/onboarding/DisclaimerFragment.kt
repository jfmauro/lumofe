package be.tivano.lumo.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import be.tivano.lumo.databinding.FragmentDisclaimerBinding

class DisclaimerFragment : Fragment() {

    private var _binding: FragmentDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.checkboxDisclaimer.setOnCheckedChangeListener { _, isChecked ->
            (activity as? OnboardingActivity)?.onDisclaimerCheckChanged(isChecked)
        }
    }

    fun isAccepted(): Boolean = binding.checkboxDisclaimer.isChecked

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
