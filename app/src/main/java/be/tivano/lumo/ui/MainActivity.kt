package be.tivano.lumo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import be.tivano.lumo.R
import be.tivano.lumo.data.RetrofitClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        setContentView(R.layout.activity_main)
        // TODO: inflate main dashboard layout (US-0.1.2+)
    }
}
