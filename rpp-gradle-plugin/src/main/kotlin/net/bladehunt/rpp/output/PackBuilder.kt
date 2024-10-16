package net.bladehunt.rpp.output

import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.util.Globs
import net.bladehunt.rpp.util.archiveDirectory
import org.slf4j.Logger
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.StandardWatchEventKinds
import java.util.regex.Pattern
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

private const val IGNORE_NAME = ".rppignore"

private const val LOCK_NAME = "rpp.lock"

private val defaultIgnores = listOf(
    Pattern.compile(Globs.toUnixRegexPattern("*/$IGNORE_NAME")),
    Pattern.compile(Globs.toUnixRegexPattern("codegen.jsonc"))
)

private fun awaitLock(buildDir: Path) {
    val lockFile = buildDir.resolve(LOCK_NAME)

    if (!lockFile.exists()) return

    val service = FileSystems.getDefault().newWatchService()
    buildDir.register(
        service,
        arrayOf(StandardWatchEventKinds.ENTRY_DELETE)
    )

    service.use {
        while (true) {
            val key = it.take()
            for (event in key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE &&
                    (event.context() as Path) == lockFile.fileName) {
                    return
                }
            }
            if (!key.reset()) break
        }
    }
}

private fun lock(buildDir: Path): FileChannel {
    val lockFile = buildDir.resolve(LOCK_NAME)
    return FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).apply {
        try {
            tryLock() ?: throw IllegalStateException("Unable to acquire lock")
        } catch (e: Exception) {
            close()
            throw e
        }
    }
}

private fun unlock(channel: FileChannel, buildDir: Path) {
    try {
        channel.close()
    } finally {
        try {
            buildDir.resolve(LOCK_NAME).deleteIfExists()
        } catch (e: Exception) {
            println("Error deleting lock file: ${e.message}")
        }
    }
}

internal fun buildResourcePack(
    logger: Logger,
    sourceDirectory: File,
    buildDir: File,
    version: String,
    extension: RppExtension
) {
    val buildPath = buildDir.toPath()
    awaitLock(buildPath)

    val channel = lock(buildPath)

    try {
        val outputDir = buildDir.resolve("output").absoluteFile

        outputDir.mkdirs()

        val context = BuildContext(
            logger, buildDir, outputDir, sourceDirectory,
            extension.fileProcessors.sortedBy { it.priority },
            extension.outputProcessors.sortedBy { it.priority },
            extension.archiveProcessors.sortedBy { it.priority }
        )

        processOutput(context)
        context.logger.debug("Build context: {}", context.toString().replace(", ", "\n"))
        context.outputProcessors.forEach { it.process(context) }

        val defaultArchive = buildDir.resolve((extension.outputName?.plus(".zip")) ?: "resource_pack_${version}.zip")
        archiveDirectory(outputDir, defaultArchive)

        context.generatedArchives.add(Archive("default", defaultArchive))

        context.archiveProcessors.forEach { it.process(context) }

        // write sha1 files
        context.generatedArchives.forEach { archive ->
            val hashFile = archive.file.parentFile.resolve(archive.file.name.removeSuffix(".zip") + ".sha1")
            hashFile.writeText(archive.sha1Hash)
        }
    } catch (e: Exception) {
        logger.error("An uncaught exception occurred in the build")
        e.printStackTrace()
    } finally {
        unlock(channel, buildPath)
    }
}

private fun processOutput(
    context: BuildContext
) {
    if (context.outputDirectory.exists() && !context.outputDirectory.deleteRecursively()) throw IllegalStateException("Failed to clean output")
    if (!context.outputDirectory.mkdir()) throw IllegalStateException("Failed to create output directory")

    val prefix = context.sourcesDirectory.path
    val ignoredFiles = ArrayList(defaultIgnores)

    context.sourcesDirectory.walkTopDown().forEach { file ->
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

        val output = context.outputDirectory.resolve(cleaned)
        file.copyTo(
            output,
            true
        )
        context.logger.debug("Outputting {} to {}", file, output)
        context.processFile(output)
    }
}
