package com.example.semestry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.semestry.ui.MoyenneCalculatorScreen
import com.example.semestry.ui.theme.SemestryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SemestryTheme {
                MoyenneCalculatorScreen()
            }
        }
    }
}
