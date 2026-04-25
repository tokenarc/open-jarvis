package com.openjarvis.intelligence

class TaskWorkingMemory {
    
    private val memory = mutableMapOf<String, String>()
    
    fun set(key: String, value: String) {
        memory[key] = value
    }
    
    fun get(key: String): String? {
        return memory[key]
    }
    
    fun interpolate(template: String): String {
        var result = template
        for ((key, value) in memory) {
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    fun clear() {
        memory.clear()
    }
    
    fun getAll(): Map<String, String> = memory.toMap()
}