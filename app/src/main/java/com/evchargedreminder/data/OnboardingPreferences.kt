package com.evchargedreminder.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface OnboardingPreferences {
    var isCompleted: Boolean
}

@Singleton
class OnboardingPreferencesImpl @Inject constructor(
    @ApplicationContext context: Context
) : OnboardingPreferences {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    override var isCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
