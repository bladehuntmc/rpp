package net.bladehunt.rpp.build

import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.text.Regex

class GitIgnore(private val ignorePatterns: List<String>) {

    private val processedPatterns = mutableListOf<IgnorePattern>()

    init {
        for (pattern in ignorePatterns) {
            val trimmedPattern = pattern.trim()

            // Ignore empty lines or comment lines
            if (trimmedPattern.isEmpty() || trimmedPattern.startsWith("#")) {
                continue
            }

            // Handle negation patterns (!)
            val isNegated = trimmedPattern.startsWith("!")
            val actualPattern = if (isNegated) trimmedPattern.substring(1) else trimmedPattern

            // Convert the .gitignore pattern to a regex
            val regexPattern = convertToRegex(actualPattern)
            processedPatterns.add(IgnorePattern(regexPattern, isNegated))
        }
    }

    private fun convertToRegex(pattern: String): Regex {
        val regexBuilder = StringBuilder()

        var i = 0
        while (i < pattern.length) {
            when (val c = pattern[i]) {
                '*' -> {
                    if (i + 1 < pattern.length && pattern[i + 1] == '*') {
                        regexBuilder.append(".*")
                        i++
                    } else {
                        regexBuilder.append("[^/]*")
                    }
                }
                '?' -> regexBuilder.append("[^/]")
                '.' -> regexBuilder.append("\\.")
                '/' -> regexBuilder.append("/")
                else -> regexBuilder.append(Pattern.quote(c.toString()))
            }
            i++
        }

        // Add trailing slash match for directory patterns
        if (pattern.endsWith("/")) {
            regexBuilder.append(".*")
        }

        return regexBuilder.toString().toRegex()
    }

    fun matches(path: String): Boolean {
        var isIgnored = false
        val normalizedPath = Paths.get(path).normalize().toString()

        for (ignorePattern in processedPatterns) {
            if (ignorePattern.regex.matches(normalizedPath)) {
                isIgnored = !ignorePattern.isNegated
            }
        }

        return isIgnored
    }

    private data class IgnorePattern(val regex: Regex, val isNegated: Boolean)
}

fun main() {
    val gitignorePatterns = listOf(
        "*.log",
        "!important.log",
        "build/",
        "*.tmp"
    )

    val gitIgnore = GitIgnore(gitignorePatterns)

    println(gitIgnore.matches("test.log"))       // Should be true
    println(gitIgnore.matches("important.log"))  // Should be false
    println(gitIgnore.matches("build/index.js")) // Should be true
    println(gitIgnore.matches("notes.tmp"))      // Should be true
    println(gitIgnore.matches("src/main.kt"))    // Should be false
    println(gitIgnore.matches("build/idk/test.json"))    // Should be false
}
