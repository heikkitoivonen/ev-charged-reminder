package com.evchargedreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.evchargedreminder.ui.navigation.AppNavHost
import com.evchargedreminder.ui.theme.EVChargedReminderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EVChargedReminderTheme {
                AppNavHost()
            }
        }
    }
}
