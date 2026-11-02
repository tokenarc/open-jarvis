package com.openjarvis.agent

/** Shared allow-list for AI patch proposals. Runtime never applies a proposal itself. */
object RepairPatchPolicy {
    fun isAllowedPath(path: String): Boolean {
        val clean = path.trim().removePrefix("a/").removePrefix("b/")
        return (clean.startsWith("app/src/main/java/com/openjarvis/") ||
            clean.startsWith("app/src/test/java/com/openjarvis/")) &&
            !clean.contains("..") && !clean.contains("euronics", ignoreCase = true)
    }

    fun validateDiff(diff: String): Boolean {
        if (diff.isBlank() || diff.length > 24000) return false
        val paths = Regex("""(?m)^(?:\+\+\+|---) ([^\t\n]+)""")
            .findAll(diff).map { it.groupValues[1] }
            .filter { it != "/dev/null" }.toList()
        return paths.isNotEmpty() && paths.all(::isAllowedPath) &&
            !diff.contains("BEGIN PRIVATE KEY") &&
            !diff.contains("local.properties")
    }
}
