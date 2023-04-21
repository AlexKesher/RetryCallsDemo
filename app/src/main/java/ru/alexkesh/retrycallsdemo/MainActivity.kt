package ru.alexkesh.retrycallsdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.alexkesh.retrycalls.R
import ru.alexkesh.retrycallsdemo.di.AppCompositionRoot
import ru.alexkesh.retrycallsdemo.endpoints.JsonPlaceholderApi
import ru.alexkesh.retrycallsdemo.goapi.impl.request

class MainActivity : AppCompatActivity() {

    private val appCompositionRoot: AppCompositionRoot
        get() = (application as RetryCallsApp).appCompositionRoot

    private val api: JsonPlaceholderApi
        get() = appCompositionRoot.jsonPlaceholderApi

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
            try {
                val post = request { api.getPostGoApiCall(5) }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "${e.javaClass.simpleName} happend", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}