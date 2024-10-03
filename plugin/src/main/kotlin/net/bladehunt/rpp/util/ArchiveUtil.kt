package net.bladehunt.rpp.util

import com.grack.nanojson.JsonWriter
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.codegen.generateFontClass
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val IGNORE_NAME = ".rppignore"

private val FONT_MATCHER = Pattern.compile("assets/\\w+/font/\\w+\\.json")

internal fun Project.buildResourcePack(
    extension: RppExtension =
        extensions.getByName("rpp") as RppExtension
) {
    val outputName = extension.outputName ?: "resource_pack_${project.version}"
    val sourceDirectory = File(extension.sourceDirectory)

    val buildDir = layout.buildDirectory.get().dir("rpp")
    buildDir.asFile.mkdirs()

    val outputDir = buildDir.dir("output").asFile
    generateOutput(sourceDirectory, buildDir.dir("generated/java").asFile, outputDir, extension)
    val output = buildDir.file("$outputName.zip").asFile
    archive(outputDir, output)
    buildDir.file("$outputName.sha1").asFile.writeText(output.sha1())
}

private val JsonParser = com.grack.nanojson.JsonParser.`object`()

fun generateOutput(
    source: File,
    generatedOutput: File,
    output: File,
    extension: RppExtension
) {
    val minifyJson = extension.minifyJson
    val fonts = extension.codegen.fonts

    if (!output.deleteRecursively()) throw IllegalStateException("Failed to clean output")
    if (!output.mkdir()) throw IllegalStateException("Failed to create output directory")

    if (!generatedOutput.deleteRecursively()) throw IllegalStateException("Failed to clean generated output")

    // TODO: update output generation
    val prefix = source.path
    val ignoredFiles = arrayListOf<Pattern>()

    source.walkTopDown().forEach { file ->
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

        if (file.name == IGNORE_NAME || ignoredFiles.any { it.matcher(cleaned).matches() }) return@forEach

        val resolved = output.resolve(cleaned)
        if (minifyJson && file.extension == "json") {
            resolved.parentFile.mkdirs()
            resolved.createNewFile()
            val obj = file.inputStream().use { stream -> JsonParser.from(stream) }

            if (extension.codegen.enable && FONT_MATCHER.matcher(cleaned).matches()) run codegen@{
                val (_, namespace) = cleaned.split("/")
                val font = file.nameWithoutExtension

                val fontObj = fonts.fonts.firstOrNull { it.font == font && it.namespace == namespace }

                if (fontObj == null) return@codegen

                val className = fonts.classPrefix +
                        font.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } +
                        fonts.classSuffix
                val clazz = generateFontClass(
                    extension.codegen.fonts.`package`,
                    className,
                    fontObj,
                    obj
                )

                val generatedFile = generatedOutput.resolve(extension.codegen.fonts.`package`.replace('.', '/') + "/" + className + ".java")
                generatedFile.parentFile.mkdirs()
                generatedFile.createNewFile()
                generatedFile.outputStream().bufferedWriter().use { out ->
                    out.write(clazz)
                }
            }

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