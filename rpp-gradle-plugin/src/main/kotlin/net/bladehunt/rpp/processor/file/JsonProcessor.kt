package net.bladehunt.rpp.processor.file

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.api.Json
import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.output.FileProcessResult
import java.io.File
import java.io.OutputStream

private val matcher = Regex(".*\\.jsonc?$")

class JsonProcessor(
    private val transpileJsonc: Boolean,
    private val minify: Boolean = false,
    priority: Int = 999
) : FileProcessor({ it.name.matches(matcher) }, priority) {
    @OptIn(ExperimentalSerializationApi::class)
    override fun processFile(context: BuildContext, source: File): FileProcessResult {
        if (!minify && !transpileJsonc) return FileProcessResult.CONTINUE

        val input: JsonObject
        val output: OutputStream
        val result: FileProcessResult

        when {
            transpileJsonc && source.extension == "jsonc" -> {
                val newFile = source.parentFile.resolve(source.nameWithoutExtension + ".json")

                if (!newFile.createNewFile()) {
                    context.logger.warn("File ${newFile.name} could not be created")
                }
                input = source.inputStream().use { input -> Json.decodeFromStream<JsonObject>(input) }
                result = FileProcessResult.DELETED
                output = newFile.outputStream()
            }

            minify && source.extension == "json" -> {
                input = source.inputStream().use { input -> Json.decodeFromStream<JsonObject>(input) }
                result = FileProcessResult.CONTINUE
                output = source.outputStream()
            }

            else -> return FileProcessResult.CONTINUE
        }

        try {
            output.use { Json.encodeToStream(input, it) }
        } catch (e: SerializationException) {
            if (e.message != null) {
                context.logger.warn("Exception while serializing JSON: {}", e.message)
            } else {
                e.printStackTrace()
            }
        }

        return result
    }
}