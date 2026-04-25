package com.openjarvis.local

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.RandomAccessFile

class ModelManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var unloadJob: Job? = null
    private var downloadJob: Job? = null
    
    private val _state = MutableStateFlow<ModelState>(ModelState.Unloaded)
    val state: StateFlow<ModelState> = _state
    
    private val modelDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }
    
    fun getModelPath(tier: ModelTier): String {
        return File(modelDir, tier.fileName).absolutePath
    }
    
    fun isModelDownloaded(tier: ModelTier): Boolean {
        val file = File(modelDir, tier.fileName)
        return file.exists() && file.length() >= tier.minSize
    }
    
    fun getDownloadedTier(): ModelTier? {
        return ModelTier.entries.find { isModelDownloaded(it) }
    }
    
    fun canLoadModel(tier: ModelTier): Boolean {
        if (!isModelDownloaded(tier)) return false
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMB = memInfo.availMem / (1024 * 1024)
        val requiredMB = tier.ramRequiredMB
        
        return availableMB > (requiredMB + 500)
    }
    
    suspend fun loadModel(tier: ModelTier): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!canLoadModel(tier)) {
                throw Exception("Not enough RAM. Need ${tier.ramRequiredMB}MB free.")
            }
            
            _state.value = ModelState.Loading
            
            resetUnloadTimer()
            
            _state.value = ModelStateLoaded(tier)
        }
    }
    
    fun unloadModel() {
        unloadJob?.cancel()
        
        scope.launch {
            _state.value = ModelState.Unloading
            
            delay(500)
            
            _state.value = ModelState.Unloaded
        }
    }
    
    private fun resetUnloadTimer() {
        unloadJob?.cancel()
        unloadJob = scope.launch {
            delay(UNLOAD_TIMEOUT_MS)
            unloadModel()
        }
    }
    
    suspend fun downloadModel(
        tier: ModelTier,
        onProgress: (Float) -> Unit
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            if (downloadJob?.isActive == true) {
                throw Exception("Download already in progress")
            }
            
            _state.value = ModelState.Downloading(tier, 0f)
            
            val file = File(modelDir, tier.fileName)
            val existingSize = if (file.exists()) file.length() else 0L
            
            val request = okhttp3.Request.Builder()
                .url(tier.downloadUrl)
                .apply {
                    if (existingSize > 0) {
                        addHeader("Range", "bytes=$existingSize-")
                    }
                }
                .build()
            
            val client = okhttp3.OkHttpClient()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful && response.code != 206 && response.code != 200) {
                throw Exception("Download failed: HTTP ${response.code}")
            }
            
            val totalSize = response.header("Content-Length")?.toLongOrNull()?.let { existingSize + it } 
                ?: tier.minSize
            val body = response.body ?: throw Exception("Empty response")
            
            body.byteStream().use { input ->
                file.outputStream().let { output ->
                    if (existingSize > 0) {
                        output.channel.position(existingSize)
                    }
                    
                    val buffer = ByteArray(8192)
                    var downloaded = existingSize
                    var lastProgress = 0f
                    
                    while (true) {
                        val bytes = input.read(buffer)
                        if (bytes <= 0) break
                        
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        
                        val progress = downloaded.toFloat() / totalSize
                        if (progress - lastProgress > 0.01f) {
                            _state.value = ModelState.Downloading(tier, progress)
                            onProgress(progress)
                            lastProgress = progress
                        }
                    }
                }
            }
            
            _state.value = ModelState.Downloaded(tier)
            
            totalSize
        }
    }
    
    enum class ModelTier(
        val displayName: String,
        val fileName: String,
        val downloadUrl: String,
        val minSize: Long,
        val ramRequiredMB: Int,
        val description: String
    ) {
        MINIMUM(
            "Gemma 2B",
            "gemma-2-2b-q5.gguf",
            "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-q5_k_m.gguf",
            1_800_000_000,
            1800,
            "Fast, 1.8GB, good for daily tasks"
        ),
        BALANCED(
            "Phi-3 Mini",
            "phi-3-mini-q4.gguf",
            "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-GGUF/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
            3_800_000_000,
            2500,
            "Balanced, 3.8GB, best accuracy"
        ),
        POWER(
            "Llama 3 8B",
            "llama-3-8b-q5.gguf",
            "https://huggingface.co/bartowski/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct-Q5_K_M.gguf",
            4_800_000_000,
            3500,
            "Powerful, 4.8GB, complex reasoning"
        );
        
        companion object {
            fun fromName(name: String): ModelTier? {
                return entries.find { it.displayName.equals(name, ignoreCase = true) }
            }
        }
    }
    
    sealed class ModelState {
        object Unloaded : ModelState()
        data class Loading(val tier: ModelTier) : ModelState()
        data class Loaded(val tier: ModelTier) : ModelState()
        object Unloading : ModelState()
        data class Downloading(val tier: ModelTier, val progress: Float) : ModelState()
        data class Downloaded(val tier: ModelTier) : ModelState()
    }
    
    companion object {
        private const val UNLOAD_TIMEOUT_MS = 5 * 60 * 1000L
    }
}