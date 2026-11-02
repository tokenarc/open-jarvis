package com.openjarvis.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.TextView
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import com.openjarvis.R
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import com.openjarvis.bridge.SocketServer
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.overlay.FloatingOverlayWidget
import com.openjarvis.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    private lateinit var agentCore: AgentCore
    private lateinit var voiceManager: VoiceManager
    private lateinit var graphifyRepo: GraphifyRepository
    private lateinit var socketServer: SocketServer
    private var stateCollectJob: Job? = null
    private var recentTasks = emptyList<TaskNode>()
    
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        
        agentCore = AgentCore(this)
        voiceManager = VoiceManager(this)
        graphifyRepo = GraphifyRepository(this)
        socketServer = SocketServer(this, agentCore, graphifyRepo)
        
        startForeground(NOTIFICATION_ID, createNotification())
        attachFloatingPill()
        
        CoroutineScope(Dispatchers.Main).launch {
            delay(0)
            initializeServices()
        }
        
        socketServer.start()
        
        // Load recent tasks
        CoroutineScope(Dispatchers.IO).launch {
            recentTasks = graphifyRepo.getRecentTasks(10)
        }
    }
    
    private suspend fun initializeServices() {
        if (isInitialized) return
        isInitialized = true
        
        // Heavy initializations deferred here
        withContext(Dispatchers.IO) {
            try {
                graphifyRepo.getRecentTasks(10)
            } catch (e: Exception) {
                // Graphify initialization
            }
        }
        
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stateCollectJob?.cancel()
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) { }
        overlayView = null
        socketServer.stop()
        voiceManager.release()
    }

    private fun attachFloatingPill() {
        if (!android.provider.Settings.canDrawOverlays(this) || overlayView != null) return
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val pill = TextView(this).apply {
                text = "●  JARVIS"
                textSize = 13f
                setTextColor(Color.WHITE)
                setPadding(24, 14, 24, 14)
                background = GradientDrawable().apply {
                    setColor(Color.rgb(35, 25, 65))
                    cornerRadius = 48f
                    setStroke(2, Color.rgb(145, 90, 255))
                }
                elevation = 12f
                contentDescription = "Open Jarvis assistant"
                setOnClickListener {
                    val launch = Intent(this@OverlayService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(launch)
                }
            }
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 220
            }
            windowManager?.addView(pill, params)
            overlayView = pill
        } catch (e: Exception) {
            android.util.Log.e("OpenJarvis", "Unable to attach floating pill", e)
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun getAgentCore() = agentCore
    fun getVoiceManager() = voiceManager
    fun getRecentTasks() = recentTasks

    fun executeCommand(command: String) {
        agentCore.executeTask(command)
        
        // Refresh tasks after execution
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            recentTasks = graphifyRepo.getRecentTasks(10)
        }
    }

    companion object {
        private const val CHANNEL_ID = "jarvis_overlay_channel"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startForegroundService(intent)
        }
    }
}