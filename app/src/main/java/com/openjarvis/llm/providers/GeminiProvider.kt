package com.openjarvis.llm.providers

import com.openjarvis.llm.ConnectionResult
import com.openjarvis.llm.HttpClient
import com.openjarvis.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash"
) : LLMProvider {
    
    override val name: String = "Google Gemini"
    override val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fullPrompt = "$systemPrompt\n\nUser: $userMessage"
            
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", fullPrompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 1024)
                })
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/models/$model:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            HttpClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(body)
                if (json.has("candidates")) {
                    json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                } else if (json.has("error")) {
                    throw Exception(json.getJSONObject("error").getString("message"))
                } else {
                    throw Exception("Unknown response: $body")
                }
            }
        }
    }
    
    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.currentTimeMillis()
            val request = Request.Builder()
                .url("$baseUrl/models?key=$apiKey")
                .get()
                .build()
            
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
            }
            System.currentTimeMillis() - startTime
        }
    }
}