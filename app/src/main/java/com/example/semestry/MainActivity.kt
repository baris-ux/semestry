package com.example.semestry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.semestry.ads.RewardedAdManager
import com.example.semestry.ui.MoyenneCalculatorScreen
import com.example.semestry.ui.theme.SemestryTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {
            RewardedAdManager.load(this)
        }
        enableEdgeToEdge()
        setContent {
            SemestryTheme {
                MoyenneCalculatorScreen()
            }
        }
    }
}
