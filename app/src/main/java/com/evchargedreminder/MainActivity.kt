package com.evchargedreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.evchargedreminder.data.OnboardingPreferences
import com.evchargedreminder.service.ChargingNotificationManager
import com.evchargedreminder.ui.navigation.AppNavHost
import com.evchargedreminder.ui.theme.EVChargedReminderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showOverride = intent.getBooleanExtra(
            ChargingNotificationManager.EXTRA_SHOW_OVERRIDE, false
        )
        val onboardingCompleted = onboardingPreferences.isCompleted
        setContent {
            EVChargedReminderTheme {
                AppNavHost(
                    showOverride = showOverride,
                    onboardingCompleted = onboardingCompleted
                )
            }
        }
    }
}
