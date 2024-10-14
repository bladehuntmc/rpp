package net.bladehunt.rpp.processor.file

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.Json
import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.output.FileProcessResult
import java.io.File

class JsonProcessor(
    private val transpileJsonc: Boolean,
    private val minify: Boolean = false,
    priority: Int = 999
) : FileProcessor({
    it.extension == "json" || (transpileJsonc && it.extension == "jsonc")
}, priority) {
    @OptIn(ExperimentalSerializationApi::class)
    override fun processFile(context: BuildContext, source: File): FileProcessResult {
        if (transpileJsonc && source.extension == "jsonc") {
            val newFile = source.parentFile.resolve(source.nameWithoutExtension + ".json")

            if (!newFile.createNewFile()) {
                context.logger.warn("File ${newFile.name} could not be created")
                return FileProcessResult.CONTINUE
            }

            try {
                newFile.outputStream().use { output ->
                    Json.encodeToStream(
                        source.inputStream().use { input -> Json.decodeFromStream<JsonObject>(input) },
                        output
                    )
                }
            } catch (e: SerializationException) {
                if (e.message != null) {
                    context.logger.warn("Exception while processing JSON: " + e.message)
                } else {
                    e.printStackTrace()
                }
            }

            source.delete()

            return FileProcessResult.DELETED
        }

        if (minify) {
            try {
                val obj = source.inputStream().use { input -> Json.decodeFromStream<JsonObject>(input) }

                source.outputStream().use { output ->
                    Json.encodeToStream(
                        obj,
                        output
                    )
                }
            } catch (e: SerializationException) {
                if (e.message != null) {
                    context.logger.warn("Exception while processing JSON: " + e.message)
                } else {
                    e.printStackTrace()
                }
            }
        }

        return FileProcessResult.CONTINUE
    }
}