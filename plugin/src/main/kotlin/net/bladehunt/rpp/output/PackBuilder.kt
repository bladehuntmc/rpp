package net.bladehunt.rpp.output

import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.util.Globs
import net.bladehunt.rpp.util.archiveDirectory
import org.slf4j.Logger
import java.io.File
import java.util.regex.Pattern

private const val IGNORE_NAME = ".rppignore"

private val defaultIgnores = listOf(
    Pattern.compile(Globs.toUnixRegexPattern("*/$IGNORE_NAME")),
    Pattern.compile(Globs.toUnixRegexPattern("codegen.json"))
)

internal fun buildResourcePack(
    logger: Logger,
    sourceDirectory: File,
    buildDir: File,
    version: String,
    extension: RppExtension
) {
    val outputDir = buildDir.resolve("output").absoluteFile

    outputDir.mkdirs()

    val context = BuildContext(
        logger, buildDir, outputDir, sourceDirectory,
        extension.fileProcessors.sortedBy { it.priority },
        extension.outputProcessors.sortedBy { it.priority },
        extension.archiveProcessors.sortedBy { it.priority }
    )

    processOutput(context)
    context.outputProcessors.forEach { it.process(context) }

    val defaultArchive = buildDir.resolve(extension.outputName ?: "resource_pack_${version}.zip")
    archiveDirectory(outputDir, defaultArchive)

    context.generatedArchives.add(Archive("default", defaultArchive))

    context.archiveProcessors.forEach { it.process(context) }

    // write sha1 files
    context.generatedArchives.forEach { archive ->
        val hashFile = archive.file.parentFile.resolve(archive.file.nameWithoutExtension + ".sha1")
        hashFile.writeText(archive.sha1Hash)
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
        val cleaned = file.path.removePrefix(prefix).removePrefix("/")

        if (file.isDirectory) {
            val ignore = file.resolve(IGNORE_NAME)

            if (ignore.exists()) {
                ignore.readLines().forEach lines@{ line ->
                    if (line.startsWith("#") || line.isEmpty()) return@lines
                    val pattern = Globs.toUnixRegexPattern(
                        if (cleaned.isEmpty()) line.removePrefix("/")
                        else "$cleaned/${line.removePrefix("/")}"
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
        context.processFile(output)
    }
}
