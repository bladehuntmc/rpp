package net.bladehunt.rpp.util

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import net.bladehunt.rpp.RppExtension
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val IGNORE_NAME = ".rppignore"

internal fun Project.buildResourcePack(
    extension: RppExtension =
        extensions.getByName("rpp") as RppExtension
) {
    val outputName = extension.outputName ?: "resource_pack_${project.version}"
    val sourceDirectory = File(extension.sourceDirectory)

    val buildDir = layout.buildDirectory.get().dir("rpp")
    buildDir.asFile.mkdirs()

    val outputDir = buildDir.dir("output").asFile
    generateOutput(sourceDirectory, outputDir)
    val output = buildDir.file("$outputName.zip").asFile
    archive(outputDir, output)
    buildDir.file("$outputName.sha1").asFile.writeText(output.sha1())
}

private val JsonParser = com.grack.nanojson.JsonParser.`object`()

fun generateOutput(source: File, output: File, minifyJson: Boolean = true) {
    if (!output.deleteRecursively()) throw IllegalStateException("Failed to clean output")
    if (!output.mkdir()) throw IllegalStateException("Failed to create output directory")

    // TODO: update output generation
    val prefix = source.path
    val ignoredFiles = arrayListOf<Pattern>()
    source.walkTopDown().forEach { file ->
        val cleaned = file.path.removePrefix(prefix).removePrefix("/")

        if (file.isDirectory) {
            val ignore = file.resolve(IGNORE_NAME)

            // output.resolve(cleaned).mkdir()

            if (ignore.exists()) {
                ignore.readLines().forEach lines@{ line ->
                    if (line.startsWith("#") || line.isEmpty()) return@lines
                    val pattern = Globs.toUnixRegexPattern(
                        if (cleaned.isEmpty()) line.removePrefix("/")
                        else "$cleaned/${line.removePrefix("/")}"
                    )
                    println(
                        if (cleaned.isEmpty()) line.removePrefix("/")
                        else "$cleaned/${line.removePrefix("/")}"
                    )
                    ignoredFiles.add(Pattern.compile(pattern))
                }
            }

            return@forEach
        }

        if (file.name == IGNORE_NAME || ignoredFiles.any { it.matcher(cleaned).matches() }) return@forEach

        val resolved = output.resolve(cleaned)
        if (minifyJson && file.extension == "json") {
            resolved.parentFile.mkdirs()
            resolved.createNewFile()
            val obj = file.inputStream().use { stream -> JsonParser.from(stream) }
            resolved.outputStream().use { out ->
                JsonWriter.on(out).`object`(obj).done()
            }
        } else file.copyTo(
            resolved,
            true
        )
    }
}

fun archive(source: File, output: File) {
    if (!source.exists() || !source.isDirectory) {
        throw IllegalStateException("Source must exist and be a directory.")
    }

    if (!source.resolve("pack.mcmeta").exists())
        throw IllegalStateException("Resource pack source must contain pack.mcmeta")

    ZipOutputStream(output.outputStream().buffered()).use { zos ->
        source.walkTopDown().forEach { file ->
            val zipFileName = file.absolutePath.removePrefix(source.absolutePath).removePrefix("/")
            val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
            zos.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().use { fis -> fis.copyTo(zos) }
            }
        }
    }
}

fun File.sha1(): String = MessageDigest.getInstance("SHA-1").let { sha1 ->
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            sha1.update(buffer, 0, read)
        }
    }

    sha1.digest().joinToString("") { "%02x".format(it) }
}