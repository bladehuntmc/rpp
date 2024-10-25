package net.bladehunt.rpp.processor

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.api.Json
import net.bladehunt.rpp.build.BuildScope
import net.bladehunt.rpp.build.FileData
import net.bladehunt.rpp.util.readJsonOrNull

private val IS_JSON = Regex(".*\\.jsonc?")

private val IS_MCMETA = Regex(".*\\.mcmeta(\\.jsonc?)?")

class JsonProcessor : FileProcessor<Nothing?> {
    override fun createContext(): Nothing? = null

    override fun BuildScope.shouldExecute(file: FileData, context: Nothing?): Boolean =
        file.source.name.matches(IS_JSON) || file.source.name.matches(IS_MCMETA)

    override fun BuildScope.process(file: FileData, context: Nothing?) {
        file.outputs.iterator().forEach { input ->
            val name = input.name
            val newFile = input.resolveSibling(
                when {
                    name.matches(IS_JSON) -> "${input.nameWithoutExtension}.json"
                    name.matches(IS_MCMETA) -> "${input.nameWithoutExtension.removeSuffix(".mcmeta")}.mcmeta"
                    else -> return@forEach
                }
            )

            val obj = input.readJsonOrNull<JsonObject>() ?: return@forEach

            if (input.name != newFile.name) {
                file.outputs.remove(input)
                input.delete()
                newFile.createNewFile()
                file.outputs.add(newFile)
            }

            newFile.outputStream().use { Json.encodeToStream(obj, it) }
        }
    }
}

fun RppExtension.processJson() {
    fileProcessors.add(JsonProcessor())
}