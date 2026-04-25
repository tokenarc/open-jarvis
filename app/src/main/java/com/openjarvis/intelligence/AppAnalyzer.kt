package com.openjarvis.intelligence

import android.content.Context
import android.content.pm.PackageManager
import com.openjarvis.graphify.GraphifyDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppAnalyzer(private val context: Context) {
    
    private val db = GraphifyDB.getInstance(context)
    private val dao = db.appIntelligenceDao()
    
    suspend fun analyzeAllApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.enabled && it.packageName != context.packageName }
        
        val entities = installedApps.mapNotNull { info ->
            val packageName = info.packageName
            val appName = info.loadLabel(pm).toString()
            
            val knownAI = AIApps.KNOWN_AI_APPS[packageName]
            if (knownAI != null) {
                return@mapNotNull AppIntelligenceEntity(
                    packageName = packageName,
                    appName = appName,
                    category = categorizeApp(appName, packageName),
                    capabilities = detectCapabilities(packageName, appName).joinToString(","),
                    trustScore = 0.5f,
                    lastAnalyzed = System.currentTimeMillis(),
                    isAIApp = true,
                    aiMeta = knownAI.appName
                )
            }
            
            if (packageName in AIApps.POPULAR_APPS) {
                AppIntelligenceEntity(
                    packageName = packageName,
                    appName = appName,
                    category = categorizeApp(appName, packageName),
                    capabilities = detectCapabilities(packageName, appName).joinToString(","),
                    trustScore = 0.6f,
                    lastAnalyzed = System.currentTimeMillis(),
                    isAIApp = false
                )
            } else null
        }
        
        dao.insertAll(entities)
    }
    
    suspend fun onAppInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (e: Exception) { return@withContext }
        
        val appName = info.loadLabel(pm).toString()
        val knownAI = AIApps.KNOWN_AI_APPS[packageName]
        
        val entity = AppIntelligenceEntity(
            packageName = packageName,
            appName = appName,
            category = categorizeApp(appName, packageName),
            capabilities = detectCapabilities(packageName, appName).joinToString(","),
            trustScore = 0.5f,
            lastAnalyzed = System.currentTimeMillis(),
            isAIApp = knownAI != null,
            aiMeta = knownAI?.appName
        )
        
        dao.insert(entity)
    }
    
    suspend fun updateTrustScore(packageName: String, success: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getByPackage(packageName) ?: return@withContext
        val newScore = if (success) {
            (current.trustScore + 0.02f).coerceAtMost(1.0f)
        } else {
            (current.trustScore - 0.05f).coerceAtLeast(0.1f)
        }
        dao.updateTrustScore(packageName, newScore)
    }
    
    suspend fun getAnalyzedCount(): Int = dao.getCount()
    suspend fun getAICount(): Int = dao.getAICount()
    suspend fun getAIApps(): List<AppIntelligenceEntity> = dao.getAIApps()
    suspend fun getAllSortedByTrust(): List<AppIntelligenceEntity> = dao.getAllSortedByTrust()
    
    private fun categorizeApp(appName: String, packageName: String): String {
        val name = appName.lowercase()
        val pkg = packageName.lowercase()
        
        return when {
            pkg.contains("messaging") || name.contains("message") || name.contains("sms") -> "COMMUNICATION"
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") -> "COMMUNICATION"
            pkg.contains("social") || pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("twitter") -> "SOCIAL"
            pkg.contains("photo") || pkg.contains("camera") || pkg.contains("gallery") -> "ENTERTAINMENT"
            pkg.contains("music") || pkg.contains("spotify") || pkg.contains("youtube") -> "ENTERTAINMENT"
            pkg.contains("maps") || pkg.contains("navigation") || pkg.contains("waze") -> "UTILITIES"
            pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("firefox") -> "BROWSER"
            pkg.contains("drive") || pkg.contains("docs") || pkg.contains("sheets") || pkg.contains("office") -> "PRODUCTIVITY"
            pkg.contains("keep") || pkg.contains("note") || pkg.contains("evernote") -> "NOTES"
            pkg.contains("calendar") -> "CALENDAR"
            pkg.contains("gmail") || pkg.contains("email") -> "EMAIL"
            pkg.contains("shop") || pkg.contains("amazon") || pkg.contains("ebay") -> "SHOPPING"
            pkg.contains("health") || pkg.contains("fit") || pkg.contains("wearable") -> "HEALTH"
            pkg.contains("bank") || pkg.contains("pay") || pkg.contains("wallet") -> "FINANCE"
            pkg.contains("ai.") || name.contains("gpt") || name.contains("gemini") || name.contains("claude") || name.contains("copilot") -> "PRODUCTIVITY"
            else -> "UNKNOWN"
        }
    }
    
    private fun detectCapabilities(packageName: String, appName: String): List<Capability> {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()
        val caps = mutableListOf<Capability>()
        
        when {
            pkg.contains("ai.") || name.contains("gpt") || name.contains("gemini") || name.contains("claude") -> {
                caps.add(Capability.AI_REASONING)
                caps.add(Capability.VOICE_INPUT)
                caps.add(Capability.VISION_OCR)
            }
        }
        
        if (pkg.contains("browser") || name.contains("browser") || name.contains("web")) {
            caps.add(Capability.WEB_BROWSE)
            caps.add(Capability.SEARCH)
        }
        
        if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("message") || pkg.contains("messenger")) {
            caps.add(Capability.MESSAGING)
        }
        
        if (name.contains("notes") || name.contains("keep") || name.contains("evernote")) {
            caps.add(Capability.NOTE_TAKING)
            caps.add(Capability.FILE_WRITE)
        }
        
        if (pkg.contains("maps") || pkg.contains("navigation") || pkg.contains("waze")) {
            caps.add(Capability.NAVIGATION)
        }
        
        if (pkg.contains("camera") || pkg.contains("photo") || pkg.contains("gallery")) {
            caps.add(Capability.IMAGE_CAPTURE)
            caps.add(Capability.FILE_READ)
        }
        
        if (caps.isEmpty()) caps.add(Capability.SEARCH)
        
        return caps
    }
}