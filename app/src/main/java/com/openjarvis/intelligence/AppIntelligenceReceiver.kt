package com.openjarvis.intelligence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppIntelligenceReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val analyzer = AppAnalyzer(context)
                    analyzer.onAppInstalled(packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}