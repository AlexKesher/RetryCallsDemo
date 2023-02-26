package ru.alexkesh.retrycallsdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.alexkesh.retrycalls.R

class MainActivity : AppCompatActivity() {

    private val api get() = (application as RetryCallsApplication).api

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            makeRequest()
        }
    }

    private fun makeRequest() {
        lifecycleScope.launch {
            val response = api.getPost(1)
            println(response.body())
        }
    }
}