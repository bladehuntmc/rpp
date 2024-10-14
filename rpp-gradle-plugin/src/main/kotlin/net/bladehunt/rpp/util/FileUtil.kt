@file:JvmName("FileUtil")

package net.bladehunt.rpp.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.rpp.api.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun archiveDirectory(source: File, output: File) {
    if (!source.exists() || !source.isDirectory) {
        throw IllegalStateException("Source must exist and be a directory.")
    }

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

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> File.readJsonOrNull(): T? {
    return try {
        inputStream().use { Json.decodeFromStream<T>(it) }
    } catch (e: SerializationException) {
        e.printStackTrace()
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}
