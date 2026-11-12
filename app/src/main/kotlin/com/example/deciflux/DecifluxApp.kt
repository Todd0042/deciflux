package com.example.deciflux

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.deciflux.data.BatchQueueRepository
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DecifluxApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Restore the options saved last time before any screen is shown (a small SharedPreferences read)
        BatchQueueRepository.restoreSavedConfig(this)

        // Safe asynchronous initialization so app startup is instant and never crashes
        appScope.launch {
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().init(this@DecifluxApp)
                FFmpeg.getInstance().init(this@DecifluxApp)
                isFfmpegInitialized = true
                Log.i(TAG, "Deciflux native FFmpeg engine & shared packages initialized successfully.")
            } catch (t: Throwable) {
                isFfmpegInitialized = false
                Log.e(TAG, "Failed to initialize native FFmpeg engine: ${t.message}", t)
            }
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

        @Volatile
        var isFfmpegInitialized: Boolean = false
            private set

        lateinit var instance: DecifluxApp
            private set
    }
}
