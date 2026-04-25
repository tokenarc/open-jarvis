package com.openjarvis.builder

import android.content.Context
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppBuilderMode(private val context: Context) {
    
    private val outputDir = File(context.filesDir, "built_apps").also { it.mkdirs() }
    private val llm = UniversalAdapter(context)
    
    private val _state = MutableStateFlow<BuildState>(BuildState.Idle)
    val state = _state
    
    sealed class BuildState {
        object Idle : BuildState()
        data class Planning(val summary: String) : BuildState()
        data class Generating(val file: String, val progress: Int) : BuildState()
        data class Building(val percent: Int) : BuildState()
        object Installing : BuildState()
        data class Complete(val appName: String, val apkPath: String?) : BuildState()
        data class Error(val message: String, val partialPath: String?) : BuildState()
    }
    
    suspend fun analyzeRequirements(prompt: String): AppSpec = withContext(Dispatchers.IO) {
        _state.value = BuildState.Planning("Analyzing requirements...")
        
        val systemPrompt = """
            You are an Android app architect.
            User describes an app idea. Extract:
            1. App name
            2. Core features (max 5)
            3. Screens needed (list each screen and its purpose)
            4. Data to store (what Room entities are needed)
            5. External services needed (camera, location, internet, etc)
            6. Complexity: SIMPLE (1-3 screens) / MEDIUM (4-6) / COMPLEX (7+)
            
            Respond ONLY with JSON matching AppSpec schema.
        """.trimIndent()
        
        val result = llm.complete(systemPrompt, prompt)
        val json = result.getOrNull() ?: throw Exception("Failed to analyze requirements")
        
        _state.value = BuildState.Idle
        parseAppSpec(JSONObject(json.trim()))
    }
    
    suspend fun generateProject(spec: AppSpec) = withContext(Dispatchers.IO) {
        val projectDir = File(outputDir, spec.packageSafeName).also { it.mkdirs() }
        
        _state.value = BuildState.Generating("build.gradle", 0)
        
        val filesToGenerate = buildFileList(spec)
        
        filesToGenerate.forEachIndexed { index, fileSpec ->
            _state.value = BuildState.Generating(
                fileSpec.path,
                (index * 100 / filesToGenerate.size)
            )
            
            val content = generateFileContent(fileSpec, spec)
            val file = File(projectDir, fileSpec.path)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }
        
        _state.value = BuildState.Building(0)
        
        if (isGradleAvailable()) {
            buildApk(projectDir)
        } else {
            zipProject(projectDir)
            _state.value = BuildState.Complete(spec.appName, null)
        }
    }
    
    private fun buildFileList(spec: AppSpec): List<FileSpec> {
        return listOf(
            FileSpec("build.gradle", FileType.GRADLE),
            FileSpec("app/build.gradle", FileType.GRADLE_APP),
            FileSpec("app/src/main/AndroidManifest.xml", FileType.MANIFEST),
            FileSpec("app/src/main/java/com/jarvisbuilt/${spec.packageSafeName}/MainActivity.kt", FileType.KOTLIN),
            FileSpec("app/src/main/java/com/jarvisbuilt/${spec.packageSafeName}/ui/theme/Theme.kt", FileType.KOTLIN),
            FileSpec("app/src/main/java/com/jarvisbuilt/${spec.packageSafeName}/data/AppDatabase.kt", FileType.KOTLIN),
            FileSpec("app/src/main/res/values/strings.xml", FileType.XML),
            FileSpec("app/src/main/res/values/colors.xml", FileType.XML),
            FileSpec("settings.gradle", FileType.GRADLE),
            FileSpec("gradle.properties", FileType.PROPERTIES),
            FileSpec("local.properties", FileType.PROPERTIES),
            FileSpec("app/proguard-rules.pro", FileType.PROGUARD),
            FileSpec("README.md", FileType.MARKDOWN)
        ) + spec.screens.map { screen ->
            FileSpec(
                "app/src/main/java/com/jarvisbuilt/${spec.packageSafeName}/ui/screens/${screen.name}.kt",
                FileType.KOTLIN
            )
        }
    }
    
    private suspend fun generateFileContent(fileSpec: FileSpec, spec: AppSpec): String {
        val prompt = buildString {
            appendLine("You are generating file: ${fileSpec.path}")
            appendLine("For app: ${spec.appName} — ${spec.description}")
            appendLine("Tech stack: Kotlin, Jetpack Compose, Room, Material3")
            appendLine("App spec:")
            appendLine(JSONObject().apply {
                put("appName", spec.appName)
                put("packageName", spec.packageName)
                put("features", JSONArray(spec.features))
                put("screens", JSONArray(spec.screens.map { it.name }))
            })
            appendLine("GENERATED APP RULES:")
            appendLine("1. Kotlin + Jetpack Compose only — no XML layouts")
            appendLine("2. Material3 components only")
            appendLine("3. Dark theme by default")
            appendLine("4. Room for all data persistence")
            appendLine("5. ViewModel + StateFlow for state")
            appendLine("6. No hardcoded strings — strings.xml")
            appendLine("7. Proper error handling")
            appendLine("8. minSdk 26, targetSdk 34")
            appendLine("9. All coroutines in viewModelScope")
            appendLine("Generate the complete, working file content.")
        }
        
        val result = llm.complete(prompt, "Generate ${fileSpec.path}")
        return result.getOrNull() ?: generateStubFile(fileSpec, spec)
    }
    
    private fun generateStubFile(fileSpec: FileSpec, spec: AppSpec): String {
        return when (fileSpec.type) {
            FileType.GRADLE -> """plugins { id 'com.android.application' }"""
            FileType.GRADLE_APP -> """apply plugin: 'com.android.application'"""
            FileType.MANIFEST -> """<manifest package="com.jarvisbuilt.${spec.packageSafeName}"/>"""
            FileType.KOTLIN -> "package com.jarvisbuilt.${spec.packageSafeName}\n\nclass Generated"
            FileType.XML -> """<?xml version="1.0"?><resources/>"""
            FileType.PROPERTIES -> "android.useAndroidX=true"
            FileType.PROGUARD -> "-keep class * { *; }"
            FileType.MARKDOWN -> "# ${spec.appName}\n\nBuilt by Jarvis"
        }
    }
    
    private fun isGradleAvailable(): Boolean {
        return try {
            Runtime.getRuntime().exec("gradle --version")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun buildApk(projectDir: File) {
        _state.value = BuildState.Building(0)
    }
    
    private fun zipProject(projectDir: File) {
        _state.value = BuildState.Complete(projectDir.name, null)
    }
    
    private fun parseAppSpec(json: JSONObject): AppSpec {
        return AppSpec(
            appName = json.optString("appName", "MyApp"),
            packageName = json.optString("packageName", "com.jarvisbuilt.myapp"),
            packageSafeName = json.optString("packageSafeName", "myapp"),
            description = json.optString("description", ""),
            features = json.optJSONArray("features")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            screens = json.optJSONArray("screens")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    ScreenSpec(
                        name = obj.optString("name", "Screen"),
                        purpose = obj.optString("purpose", ""),
                        components = obj.optJSONArray("components")?.let { c ->
                            (0 until c.length()).map { c.getString(it) }
                        } ?: emptyList(),
                        navigatesTo = obj.optJSONArray("navigatesTo")?.let { n ->
                            (0 until n.length()).map { n.getString(it) }
                        } ?: emptyList()
                    )
                }
            } ?: emptyList(),
            entities = json.optJSONArray("entities")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    EntitySpec(
                        name = obj.optString("name", "Item"),
                        fields = obj.optJSONArray("fields")?.let { f ->
                            (0 until f.length()).map { j ->
                                val field = f.getJSONObject(j)
                                FieldSpec(
                                    name = field.optString("name", "id"),
                                    type = field.optString("type", "String")
                                )
                            }
                        } ?: emptyList()
                    )
                }
            } ?: emptyList(),
            permissions = json.optJSONArray("permissions")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            complexity = Complexity.valueOf(json.optString("complexity", "SIMPLE"))
        )
    }
    
    data class FileSpec(
        val path: String,
        val type: FileType
    )
    
    enum class FileType {
        GRADLE, GRADLE_APP, MANIFEST, KOTLIN, XML, PROPERTIES, MARKDOWN
    }
}

data class AppSpec(
    val appName: String,
    val packageName: String,
    val packageSafeName: String,
    val description: String,
    val features: List<String>,
    val screens: List<ScreenSpec>,
    val entities: List<EntitySpec>,
    val permissions: List<String>,
    val complexity: AppBuilderMode.Complexity
)

data class ScreenSpec(
    val name: String,
    val purpose: String,
    val components: List<String>,
    val navigatesTo: List<String>
)

data class EntitySpec(
    val name: String,
    val fields: List<FieldSpec>
)

data class FieldSpec(
    val name: String,
    val type: String
)

enum class Complexity { SIMPLE, MEDIUM, COMPLEX }