package com.openjarvis.intelligence

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardIntelligence(private val context: Context) {
    
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    fun read(): String? {
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }
    
    fun write(text: String) {
        val clip = ClipData.newPlainText("jarvis", text)
        clipboard.setPrimaryClip(clip)
    }
    
    fun analyze(): ClipboardContent {
        val text = read() ?: return ClipboardContent.Empty
        
        return when {
            text.startsWith("http") || text.startsWith("https") -> 
                ClipboardContent.Url(text)
            text.contains("@") && text.contains(".") -> 
                ClipboardContent.Email(text)
            text.all { it.isDigit() } -> 
                ClipboardContent.PhoneNumber(text)
            text.length > 200 -> 
                ClipboardContent.LongText(text.take(200))
            else -> 
                ClipboardContent.ShortText(text)
        }
    }
    
    fun hasContent(): Boolean {
        return clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount ?: 0 > 0
    }
    
    sealed class ClipboardContent {
        object Empty : ClipboardContent()
        data class Url(val url: String) : ClipboardContent()
        data class Email(val address: String) : ClipboardContent()
        data class PhoneNumber(val number: String) : ClipboardContent()
        data class LongText(val preview: String) : ClipboardContent()
        data class ShortText(val text: String) : ClipboardContent()
    }
}