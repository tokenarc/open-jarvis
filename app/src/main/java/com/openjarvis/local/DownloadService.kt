package com.openjarvis.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openjarvis.R
import com.openjarvis.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class DownloadService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    
    private val modelManager = ModelManager(this)
    
    private val _progress = MutableStateFlow(0f)
    val progress = _progress
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val tierName = intent.getStringExtra(EXTRA_TIER) ?: return START_NOT_STICKY
                startDownload(tierName)
            }
            ACTION_CANCEL -> {
                downloadJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun startDownload(tierName: String) {
        val tier = ModelManager.ModelTier.fromName(tierName) ?: run {
            stopSelf()
            return
        }
        
        startForeground(NOTIFICATION_ID, createNotification(0f))
        
        downloadJob = scope.launch {
            modelManager.downloadModel(tier) { progress ->
                _progress.value = progress
                updateNotification(progress)
            }.fold(
                onSuccess = {
                    showCompletionNotification(true)
                },
                onFailure = { e ->
                    showErrorNotification(e.message ?: "Download failed")
                }
            )
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun createNotification(progress: Float): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_LOW
        )
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading model...")
            .setContentText("${(progress * 100).toInt()}% complete")
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, (progress * 100).toInt(), false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(progress: Float) {
        val notification = createNotification(progress)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompletionNotification(success: Boolean) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (success) "Download complete" else "Download failed")
            .setContentText(if (success) "Model ready to use" else "Please try again")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        manager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }
    
    private fun showErrorNotification(message: String) {
        showCompletionNotification(false)
    }
    
    override fun onDestroy() {
        downloadJob?.cancel()
        scope.cancel()
    }
    
    companion object {
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        
        const val ACTION_DOWNLOAD = "com.openjarvis.DOWNLOAD_MODEL"
        const val ACTION_CANCEL = "com.openjarvis.CANCEL_DOWNLOAD"
        const val EXTRA_TIER = "tier"
        
        fun startDownload(context: Context, tier: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_TIER, tier)
            }
            context.startForegroundService(intent)
        }
        
        fun cancelDownload(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}