package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.RunvantaViewModel
import com.example.ui.navigation.RunvantaNavGraph
import com.example.ui.theme.RunvantaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RunvantaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initializeAds(this)

        setContent {
            RunvantaTheme(darkTheme = true) {
                RunvantaNavGraph(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as RunvantaApp).soundManager.onResume()
    }

    override fun onPause() {
        (application as RunvantaApp).soundManager.onPause()
        super.onPause()
    }
}
