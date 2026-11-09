package com.example.deciflux

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.yausername.ffmpeg.FFmpeg

class DecifluxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Native FFmpeg Engine
        try {
            FFmpeg.getInstance().init(this)
            Log.i(TAG, "Deciflux native FFmpeg engine initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize native FFmpeg engine", e)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BATCH_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "DecifluxApp"
        const val BATCH_CHANNEL_ID = "deciflux_batch_processing"
        const val BATCH_NOTIFICATION_ID = 2026

        lateinit var instance: DecifluxApp
            private set
    }
}
