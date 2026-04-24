package com.openjarvis.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.openjarvis.R
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var expandedView: View? = null
    private var isExpanded = false
    
    private lateinit var agentCore: AgentCore
    private var stateCollectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        agentCore = AgentCore(this)
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stateCollectJob?.cancel()
        floatingView?.let { windowManager?.removeView(it) }
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

    private fun createFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_floating, null)
        
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX - (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                        val deltaY = kotlin.math.abs(event.rawY - initialTouchY)
                        if (deltaX < 10 && deltaY < 10) {
                            toggleExpanded()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(floatingView, layoutParams)
    }

    private fun toggleExpanded() {
        if (isExpanded) {
            collapseOverlay()
        } else {
            expandOverlay()
        }
        isExpanded = !isExpanded
    }

    private fun expandOverlay() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        expandedView = LayoutInflater.from(this).inflate(R.layout.overlay_expanded, null)
        
        val commandInput = expandedView?.findViewById<EditText>(R.id.command_input)
        val sendButton = expandedView?.findViewById<Button>(R.id.send_button)
        val statusText = expandedView?.findViewById<TextView>(R.id.status_text)
        val minimizeButton = expandedView?.findViewById<Button>(R.id.minimize_button)

        sendButton?.setOnClickListener {
            val command = commandInput?.text?.toString() ?: ""
            if (command.isNotBlank()) {
                statusText?.text = getString(R.string.running)
                agentCore.executeTask(command)
                commandInput.text?.clear()
                
                stateCollectJob?.cancel()
                stateCollectJob = CoroutineScope(Dispatchers.Main).launch {
                    agentCore.state.collect { state ->
                        when (state) {
                            is AgentState.Idle -> statusText?.text = getString(R.string.idle)
                            is AgentState.Running -> statusText?.text = getString(R.string.running)
                            is AgentState.Done -> {
                                statusText?.text = getString(R.string.done)
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(1500)
                                    collapseOverlay()
                                    isExpanded = false
                                }
                            }
                            is AgentState.Error -> {
                                statusText?.text = getString(R.string.error)
                            }
                        }
                    }
                }
            }
        }

        minimizeButton?.setOnClickListener {
            collapseOverlay()
            isExpanded = false
        }

        floatingView?.visibility = View.GONE
        windowManager?.addView(expandedView, layoutParams)
    }

    private fun collapseOverlay() {
        stateCollectJob?.cancel()
        expandedView?.let { windowManager?.removeView(it) }
        expandedView = null
        floatingView?.visibility = View.VISIBLE
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