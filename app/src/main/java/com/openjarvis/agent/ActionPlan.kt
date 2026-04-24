package com.openjarvis.agent

data class Action(
    val action: String,
    val packageName: String? = null,
    val label: String? = null,
    val text: String? = null,
    val value: String? = null,
    val hint: String? = null,
    val timeoutMs: Long = 3000
) {
    companion object {
        const val OPEN_APP = "open_app"
        const val TAP = "tap"
        const val TYPE = "type"
        const val LONG_PRESS = "long_press"
        const val SWIPE = "swipe"
        const val SCROLL = "scroll"
        const val PRESS_BACK = "press_back"
        const val PRESS_HOME = "press_home"
        const val PRESS_RECENTS = "press_recents"
        const val WAIT_FOR = "wait_for"
        const val SCREENSHOT = "screenshot"
        const val READ_SCREEN = "read_screen"
    }
}

sealed class AgentState {
    object Idle : AgentState()
    data class Running(val step: String) : AgentState()
    data class Done(val result: String) : AgentState()
    data class Error(val message: String) : AgentState()
}