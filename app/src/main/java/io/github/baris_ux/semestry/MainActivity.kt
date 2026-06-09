package io.github.baris_ux.semestry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.baris_ux.semestry.ads.RewardedAdManager
import io.github.baris_ux.semestry.ui.MoyenneCalculatorScreen
import io.github.baris_ux.semestry.ui.theme.SemestryTheme
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
