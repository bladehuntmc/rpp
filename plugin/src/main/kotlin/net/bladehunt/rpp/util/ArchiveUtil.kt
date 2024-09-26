package net.bladehunt.rpp.util

import net.bladehunt.rpp.RppExtension
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

fun generateOutput(source: File, output: File) {
    if (!output.deleteRecursively()) throw IllegalStateException("Failed to clean output")
    if (!output.mkdir()) throw IllegalStateException("Failed to create output directory")

    // TODO: update output generation
    if (!source.copyRecursively(output)) throw IllegalStateException("Failed to copy source to output")
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