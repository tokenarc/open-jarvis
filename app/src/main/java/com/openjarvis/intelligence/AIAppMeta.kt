package com.openjarvis.intelligence

data class AIAppMeta(
    val packageName: String,
    val appName: String,
    val inputFieldHint: String,
    val sendButtonText: String?,
    val responseExtraction: ResponseExtraction,
    val supportsVoiceInput: Boolean = false,
    val contextRetention: Boolean = true
)

enum class ResponseExtraction {
    SCREEN_TEXT,
    COPY_BUTTON,
    SHARE_MENU,
    OCR_REQUIRED
}

object AIApps {
    val KNOWN_AI_APPS = mapOf(
        "com.openai.chatgpt" to AIAppMeta(
            packageName = "com.openai.chatgpt",
            appName = "ChatGPT",
            inputFieldHint = "Ask anything...",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = true,
            contextRetention = true
        ),
        "com.google.android.apps.bard" to AIAppMeta(
            packageName = "com.google.android.apps.bard",
            appName = "Gemini",
            inputFieldHint = "Enter prompt",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = true,
            contextRetention = true
        ),
        "com.anthropic.claude" to AIAppMeta(
            packageName = "com.anthropic.claude",
            appName = "Claude",
            inputFieldHint = "Message Claude",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = true,
            contextRetention = true
        ),
        "com.perplexity.ai.client" to AIAppMeta(
            packageName = "com.perplexity.ai.client",
            appName = "Perplexity",
            inputFieldHint = "Ask anything...",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = false,
            contextRetention = false
        ),
        "com.microsoft.copilot" to AIAppMeta(
            packageName = "com.microsoft.copilot",
            appName = "Microsoft Copilot",
            inputFieldHint = "Ask Copilot",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = true,
            contextRetention = true
        ),
        "com.mistral.al" to AIAppMeta(
            packageName = "com.mistral.al",
            appName = "Mistral",
            inputFieldHint = "Send a message...",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = false,
            contextRetention = true
        ),
        "ai.kakao.aichat" to AIAppMeta(
            packageName = "ai.kakao.aichat",
            appName = "Kakao i",
            inputFieldHint = "무엇이든 물어보세요",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = true,
            contextRetention = true
        ),
        "com.characterai" to AIAppMeta(
            packageName = "com.characterai",
            appName = "Character AI",
            inputFieldHint = "Send a message...",
            sendButtonText = null,
            responseExtraction = ResponseExtraction.SCREEN_TEXT,
            supportsVoiceInput = false,
            contextRetention = true
        )
    )
    
    val POPULAR_APPS = listOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.snapchat.android",
        "com.twitter.android",
        "com.linkedin.android",
        "com.tiktok",
        "com.zhiliaoapp.musically",
        "com.google.android.gm",
        "com.google.android.apps.photos",
        "com.google.android.apps.docs",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.microsoft.office.office",
        "com.microsoft.office.word",
        "com.google.android.keep",
        "com.notion.id",
        "com.ticktask.app",
        "com.evernote",
        "com.跨飞",
        "org.telegram.messenger",
        "org.thunderdog.cheggue",
        "com.skype.raider",
        "com.discord",
        "com.slack.lite",
        "com.teams.chat",
        "com.zoom.videomeeting",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.sec.android.app.samsungapps",
        "com.android.vending",
        "com.amazon.mShop.android.shopping",
        "com.ebay.mobile",
        "com.shopify",
        "com.spotify.music",
        "com.google.android.music",
        "com.apple.android.music",
        "com.netflix.mediaclient",
        "com.amazon.avod.thetvapp",
        "com.disney.disneyplus",
        "com.hbo.max",
        "com.google.android.youtube",
        "com.vimeo",
        "com.google.android.apps.maps",
        "com.waze",
        "com.ubercab",
        "com.bolt.android",
        "com.lyft",
        "com.google.android.apps.dialer",
        "com.samsung.android.contacts",
        "com.sec.android.app.clock",
        "com.google.android.apps.recorder",
        "com.camerasource",
        "com.google.android.apps.camera",
        "com.samsung.camera",
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.amazon.mShop.android.shopping",
        "com.aliexpress",
        "com.ebay.mobile",
        "com.paytm",
        "com.google.android.apps.wallet",
        "com.samsung.android.spay",
        "com.mastercard.mobile",
        "com.rbc.mobile.abn.amoco",
        "com.citi.citimobile",
        "io.invertase.notifications",
        "com.pushwoosh.sample",
        "com.onesignal"
    )
}