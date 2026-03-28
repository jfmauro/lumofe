package be.tivano.lumo.ui.circle

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import be.tivano.lumo.R

/**
 * Stub for US-0.1.4 — Creator Consent screen.
 * This class must be replaced by the full implementation of US-0.1.4.
 */
class CreatorConsentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CIRCLE_ID = "extra_circle_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Replace with full US-0.1.4 layout
        Toast.makeText(this, getString(R.string.creator_consent_stub_message), Toast.LENGTH_LONG).show()
        finish()
    }
}
