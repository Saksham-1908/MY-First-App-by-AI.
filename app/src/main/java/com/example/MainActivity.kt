package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.TimeTrackNavApp
import com.example.ui.theme.TimeTrackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TimeTrackApplication

        setContent {
            TimeTrackTheme {
                TimeTrackNavApp(repository = app.repository)
            }
        }
    }
}
