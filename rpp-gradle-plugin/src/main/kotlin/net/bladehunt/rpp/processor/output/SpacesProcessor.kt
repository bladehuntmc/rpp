package net.bladehunt.rpp.processor.output

import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.api.Json
import net.bladehunt.rpp.model.FontDefinition
import net.bladehunt.rpp.model.FontProvider
import net.bladehunt.rpp.model.Resource
import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.processor.Processor
import kotlin.math.pow

class SpacesProcessor(
    val font: Resource,
    val amount: Int,
    priority: Int
) : Processor(priority) {
    override fun process(context: BuildContext) {
        val outputDir = context.outputDirectory.resolve("assets/${font.namespace}/font")

        outputDir.mkdirs()

        val outputFile = outputDir.resolve("${font.value.removeSuffix(".json")}.json")

        if (outputFile.exists()) {
            context.logger.warn("To generate spaces, the font must not already be defined")
            return
        }

        outputFile.createNewFile()

        outputFile.outputStream().use { out ->
            Json.encodeToStream(
                FontDefinition(
                    listOf(
                        FontProvider.Space(
                            buildMap {
                                for (i in 0..<amount) {
                                    put(Char(57344 + i).toString(), 2.0.pow(i).toFloat())
                                    put(Char(61440 + i).toString(), -2.0.pow(i).toFloat())
                                }
                            }
                        )
                    )
                ),
                out
            )
        }
    }
}