package net.bladehunt.rpp.util.ignore

import net.bladehunt.rpp.util.sha1
import org.gradle.api.logging.Logging
import java.io.File

private val LOGGER = Logging.getLogger(IgnoreFile::class.java)

data class IgnoreFile(
    val patterns: Set<IgnorePattern>,
    val negatedPatterns: Set<IgnorePattern>,
    val sha1Hash: String
) {
    companion object {
        fun fromFile(file: File, hash: String? = null): IgnoreFile? {
            if (!file.exists()) return null

            val patterns = linkedSetOf<IgnorePattern>()
            val negatedPatterns = linkedSetOf<IgnorePattern>()

            for (line in file.readLines()) {
                if (line.isEmpty()) continue
                if (line.startsWith("#")) continue
                if (line.startsWith("!")) {
                    val withoutPrefix = line.removePrefix("!")
                    if (withoutPrefix.isEmpty()) continue
                    try {
                        negatedPatterns.add(IgnorePattern(withoutPrefix))
                    } catch (e: IllegalStateException) {
                        LOGGER.error(e.message)
                    }
                    continue
                }

                try {
                    patterns.add(IgnorePattern(line))
                } catch (e: IllegalStateException) {
                    LOGGER.error(e.message)
                }
            }

            return IgnoreFile(patterns, negatedPatterns, hash ?: file.sha1())
        }
    }

    fun ignores(file: File): Boolean = ignores(file.path.replace(File.separatorChar, '/'))

    fun ignores(path: String): Boolean =
        patterns.any { it.match(path, 0) != IgnorePattern.FAIL }

    fun negates(path: String): Boolean =
        negatedPatterns.any { it.match(path, 0) != IgnorePattern.FAIL }
}