package com.evchargedreminder.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SessionServiceLauncher {
    fun startSession(sessionId: Long)
}

@Singleton
class SessionServiceLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionServiceLauncher {
    override fun startSession(sessionId: Long) {
        val intent = Intent(context, LocationMonitorService::class.java).apply {
            action = LocationMonitorService.ACTION_START_SESSION
            putExtra(LocationMonitorService.EXTRA_SESSION_ID, sessionId)
        }
        context.startForegroundService(intent)
    }
}
