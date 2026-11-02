package com.openjarvis.agent

import java.util.Locale

/** Deterministic vocabulary for common Hindi, Marathi and English commands. */
object CommandDictionary {
    private val exactAliases = mapOf(
        "back" to "press back",
        "go back" to "press back",
        "piche jao" to "press back",
        "peeche jao" to "press back",
        "वापस जाओ" to "press back",
        "मागे जा" to "press back",
        "home" to "press home",
        "go home" to "press home",
        "होम खोलो" to "press home",
        "होम वर जा" to "press home",
        "recent apps" to "press recents",
        "recent kholo" to "press recents",
        "हाल के ऐप खोलो" to "press recents",
        "अलीकडील अॅप्स उघडा" to "press recents"
    )

    private val appNames = mapOf(
        "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "व्हॉट्सअॅप", "व्हाट्स अप"),
        "youtube" to listOf("youtube", "यूट्यूब", "युट्युब"),
        "chrome" to listOf("chrome", "क्रोम"),
        "settings" to listOf("settings", "setting", "सेटिंग", "सेटिंग्स", "सेटिंग्ज"),
        "phone" to listOf("phone", "dialer", "फोन", "कॉल"),
        "camera" to listOf("camera", "कैमरा"),
        "messages" to listOf("messages", "message", "sms", "मेसेज", "संदेश"),
        "maps" to listOf("maps", "google maps", "मैप", "नकाशा")
    )

    fun normalize(input: String): String {
        val original = input.trim()
        if (original.isBlank()) return original

        val key = original.lowercase(Locale.ROOT)
            .replace(Regex("[.!?,]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        exactAliases[key]?.let { return it }

        val openWords = listOf(
            "open", "launch", "start", "kholo", "khol do", "chalu karo",
            "उघड", "उघडा", "उघडून", "खोलो", "खोल दो", "चालू करो", "चालू कर"
        )
        val asksToOpen = openWords.any { key.startsWith("$it ") || key.endsWith(" $it") || key == it }
        if (asksToOpen) {
            for ((canonical, aliases) in appNames) {
                if (aliases.any { key.contains(it) }) return "open $canonical"
            }
        }
        return original
    }
}
