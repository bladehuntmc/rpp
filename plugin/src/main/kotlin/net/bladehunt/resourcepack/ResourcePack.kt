package net.bladehunt.resourcepack

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

fun Project.resourcePack(action: Action<ResourcePack>) {
    this.tasks.create("compileResourcePack", ResourcePack::class.java, action)
}

abstract class ResourcePack : DefaultTask() {
    init {
        group = "resource pack"
        description = "Archives the resource pack and generates a hash"
    }

    @Input
    val outputName: Property<String> = project.objects.property(String::class.java).convention("resource_pack_" + project.version)

    @Input
    val sourceDirectory: Property<String> = project.objects.property(String::class.java).convention("./src/main/resourcePack")

    @TaskAction
    fun compile() {
        val buildDir = project.layout.buildDirectory.get().dir("resourcePack")
        buildDir.asFile.mkdirs()

        // source
        val source = Path(sourceDirectory.get()).toFile()

        if (!source.exists() || !source.isDirectory) {
            throw IllegalStateException("Resource pack source must exist and be a directory.")
        }

        if (!source.resolve("pack.mcmeta").exists())
            throw IllegalStateException("Resource pack source must contain pack.mcmeta")

        val output = buildDir.dir("output").asFile
        if (!output.deleteRecursively()) throw IllegalStateException("Failed to clean output")
        if (!output.mkdir()) throw IllegalStateException("Failed to create output directory")
        if (!source.copyRecursively(output)) throw IllegalStateException("Failed to copy source to output")

        val outputZipFile = buildDir.file("${outputName.get()}.zip").asFile
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            output.walkTopDown().forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(output.absolutePath).removePrefix("/")
                val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { fis -> fis.copyTo(zos) }
                }
            }
        }

        val shaFile = buildDir.file("${outputName.get()}.sha1").asFile

        val sha1 = MessageDigest.getInstance("SHA-1")
        outputZipFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sha1.update(buffer, 0, read)
            }
        }

        val sha1Hash = sha1.digest().joinToString("") { "%02x".format(it) }

        shaFile.writeText(sha1Hash)
    }
}