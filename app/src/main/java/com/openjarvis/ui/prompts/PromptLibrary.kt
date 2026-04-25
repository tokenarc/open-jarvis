package com.openjarvis.ui.prompts

object PromptLibrary {
    
    data class PromptCategory(
        val name: String,
        val prompts: List<ExamplePrompt>
    )
    
    data class ExamplePrompt(
        val text: String,
        val isFavorite: Boolean = false
    )
    
    val categories = listOf(
        PromptCategory("Social Media", listOf(
            ExamplePrompt("Post a photo from my gallery to Instagram with caption [caption]"),
            ExamplePrompt("Share my location to my WhatsApp status"),
            ExamplePrompt("Reply to the last comment on my Instagram post"),
            ExamplePrompt("Post a tweet saying [text]"),
            ExamplePrompt("Create a reel with [song] and post to story")
        )),
        
        PromptCategory("Communication", listOf(
            ExamplePrompt("Call [name] on WhatsApp"),
            ExamplePrompt("If [name] doesn't pick up, send a voice note saying [message]"),
            ExamplePrompt("Reply to all unread WhatsApp messages with automated response"),
            ExamplePrompt("Forward last message from [name] to [other name]"),
            ExamplePrompt("Schedule a message to send at [time] to [name]")
        )),
        
        PromptCategory("Productivity", listOf(
            ExamplePrompt("Take a screenshot, crop it and send to [name]"),
            ExamplePrompt("Open email, find latest from [sender], summarize it"),
            ExamplePrompt("Create a note with today's to-do list"),
            ExamplePrompt("Set 3 alarms: 7am, 8am, and 8:30am"),
            ExamplePrompt("Add [event] to calendar on [date]")
        )),
        
        PromptCategory("AI Tasks", listOf(
            ExamplePrompt("Open Gemini and ask it to write a professional bio"),
            ExamplePrompt("Take photo of text, extract it and translate to Urdu"),
            ExamplePrompt("Ask ChatGPT to review my resume in Downloads"),
            ExamplePrompt("Open Perplexity and research [topic]"),
            ExamplePrompt("Use Claude to summarize this article")
        )),
        
        PromptCategory("Device Control", listOf(
            ExamplePrompt("Turn WiFi off, enable hotspot"),
            ExamplePrompt("Lower brightness to 30%, enable DND"),
            ExamplePrompt("Clear cache of all social media apps"),
            ExamplePrompt("Find screenshots from this week and delete blurry ones"),
            ExamplePrompt("Restart Bluetooth and connect to last device")
        )),
        
        PromptCategory("Creative", listOf(
            ExamplePrompt("Record a 30 second voice memo"),
            ExamplePrompt("Find a recipe for [dish] online, save to notes"),
            ExamplePrompt("Build a simple to-do list app with checkboxes"),
            ExamplePrompt("Extract text from this image using OCR"),
            ExamplePrompt("Create a photo collage from last 5 photos")
        ))
    )
    
    fun getAllPrompts(): List<ExamplePrompt> = categories.flatMap { it.prompts }
    
    fun search(query: String): List<ExamplePrompt> {
        val q = query.lowercase()
        return getAllPrompts().filter { it.text.lowercase().contains(q) }
    }
}