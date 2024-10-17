package net.bladehunt.rpp.output

import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.processor.file.FileProcessor
import net.bladehunt.rpp.processor.Processor
import net.bladehunt.rpp.util.FileLock
import net.bladehunt.rpp.util.Globs
import net.bladehunt.rpp.util.archiveDirectory
import org.gradle.api.Task
import java.io.File
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.measureTime

private const val IGNORE_NAME = ".rppignore"

private val defaultIgnores = listOf(
    Pattern.compile(Globs.toUnixRegexPattern("*/$IGNORE_NAME")),
    Pattern.compile(Globs.toUnixRegexPattern("codegen.jsonc"))
)

data class BuildContext(
    val logger: org.gradle.api.logging.Logger,

    val buildDirectory: File,
    val sourcesDirectory: File,

    val fileProcessors: List<FileProcessor>,
    val outputProcessors: List<Processor>,
    val archiveProcessors: List<Processor>,

    val defaultArchiveName: String,
) {
    @Transient
    val generatedArchives: MutableMap<String, Archive> = linkedMapOf()

    @Transient
    val outputDirectory: File = buildDirectory.resolve("output")

    @Transient
    val lockFile = buildDirectory.resolve("rpp.lock")

    companion object {
        fun fromTask(task: Task): BuildContext {
            val extension = task.extensions.getByName("rpp") as RppExtension
            return BuildContext(
                logger = task.project.logger,
                buildDirectory = task.project.layout.buildDirectory.asFile.get().resolve("rpp"),
                sourcesDirectory = task.project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory),
                fileProcessors = extension.fileProcessors,
                outputProcessors = extension.outputProcessors,
                defaultArchiveName = extension.outputName ?: "resource_pack_${task.project.version}",
                archiveProcessors = extension.archiveProcessors
            )
        }
    }

    internal fun buildTimed(log: Boolean = true): Duration {
        val duration = measureTime(this::build)
        logger.lifecycle("Built resource pack in ${duration.inWholeMilliseconds}ms\n")
        return duration
    }

    internal fun build() = FileLock(lockFile).use {
        try {
            logger.lifecycle("Building resource pack...")

            outputDirectory.mkdirs()

            processOutput()
            logger.debug("Build context: {}", toString().replace(", ", "\n"))
            outputProcessors.forEach { it.process(this) }

            val defaultArchive = buildDirectory.resolve(defaultArchiveName.removeSuffix(".zip") + ".zip")
            archiveDirectory(outputDirectory, defaultArchive)

            generatedArchives[Archive.DEFAULT_NAME] = Archive(Archive.DEFAULT_NAME, defaultArchive)

            archiveProcessors.forEach { it.process(this) }

            // write sha1 files
            generatedArchives.forEach { (_, archive) ->
                val hashFile = archive.file.parentFile.resolve(archive.file.name.removeSuffix(".zip") + ".sha1")
                hashFile.writeText(archive.sha1Hash)
            }
        } catch (e: Exception) {
            logger.error("An uncaught exception occurred in the build")
            e.printStackTrace()
        }
    }

    internal fun clean() {
        buildDirectory.deleteRecursively()
    }

    private fun processOutput() {
        if (outputDirectory.exists() && !outputDirectory.deleteRecursively()) throw IllegalStateException("Failed to clean output")
        if (!outputDirectory.mkdir()) throw IllegalStateException("Failed to create output directory")

        val prefix = sourcesDirectory.path
        val ignoredFiles = ArrayList(defaultIgnores)

        sourcesDirectory.walkTopDown().forEach { file ->
            val cleaned = file.path.removePrefix(prefix).removePrefix(File.separator)

            if (file.isDirectory) {
                val ignore = file.resolve(IGNORE_NAME)

                if (ignore.exists()) {
                    ignore.readLines().forEach lines@{ line ->
                        if (line.startsWith("#") || line.isEmpty()) return@lines
                        val pattern = Globs.toUnixRegexPattern(
                            if (cleaned.isEmpty()) line.removePrefix(File.separator)
                            else "$cleaned${File.separator}${line.removePrefix(File.separator)}"
                        )
                        ignoredFiles.add(Pattern.compile(pattern))
                    }
                }

                return@forEach
            }

            if (ignoredFiles.any { it.matcher(cleaned).matches() }) return@forEach

            val output = outputDirectory.resolve(cleaned)
            file.copyTo(
                output,
                true
            )
            logger.debug("Outputting {} to {}", file, output)
            processFile(output)
        }
    }

    /**
     * Processes the file, stopping after it is deleted.
     *
     * @return True if successfully completed without deletion, otherwise false
     */
    fun processFile(file: File): Boolean {
        fileProcessors.forEach {
            if (it.fileFilter.accept(file)) {
                if (it.processFile(this, file) != FileProcessResult.CONTINUE) return false
            }
        }
        return true
    }
}