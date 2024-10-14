package net.bladehunt.rpp.processor.file

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.api.Json
import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.output.FileProcessResult
import net.bladehunt.rpp.util.readJsonOrNull
import java.io.File
import java.io.FileFilter

private val matcher = Regex(".*\\.mcmeta\\.jsonc?$")
private val replacement = Regex("\\.jsonc?$")

class McmetaProcessor(priority: Int) : FileProcessor(FileFilter { it.name.matches(matcher) }, priority) {
    override fun processFile(context: BuildContext, source: File): FileProcessResult {
        val packDefinition = source.readJsonOrNull<JsonObject>() ?: return FileProcessResult.CONTINUE

        val newFile = source.parentFile.resolve(source.name.replace(replacement, ""))
        newFile.createNewFile()
        newFile.outputStream().use { Json.encodeToStream(packDefinition, it) }

        context.processFile(newFile)

        source.delete()
        return FileProcessResult.DELETED
    }
}