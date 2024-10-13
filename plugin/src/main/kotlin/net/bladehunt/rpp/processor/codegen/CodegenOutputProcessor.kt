package net.bladehunt.rpp.processor.codegen

import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.processor.Processor
import net.bladehunt.rpp.util.readJsonOrNull

internal const val CODEGEN_FILENAME = "codegen.jsonc"

class CodegenOutputProcessor(
    priority: Int,
    generators: Collection<CodeGenerator>
) : Processor(priority) {
    private val generators = generators.sortedBy { it.priority }

    override fun process(context: BuildContext) {
        val configFile = context.sourcesDirectory.resolve(CODEGEN_FILENAME)
        if (!configFile.exists()) {
            context.logger.warn("codegen.jsonc was not found. Skipping codegen...")
            return
        }

        val config = configFile.readJsonOrNull<CodegenConfig>() ?: return
        val codegenOutput = context.buildDirectory.resolve("generated/java")

        codegenOutput.deleteRecursively()

        codegenOutput.mkdirs()

        if (config.fonts.isNotEmpty()) FontGenerator.generate(context, config, codegenOutput)

        if (config.spaces.isNotEmpty()) SpacesGenerator.generate(context, config, codegenOutput)

        generators.forEach {
            it.generate(context, config, codegenOutput)
        }
    }
}