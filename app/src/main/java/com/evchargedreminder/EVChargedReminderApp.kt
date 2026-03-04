package com.evchargedreminder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.evchargedreminder.service.BootCompletedReceiver
import com.evchargedreminder.service.ChargingNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EVChargedReminderApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationManager: ChargingNotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannels()
        BootCompletedReceiver.scheduleGeofenceRefresh(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
