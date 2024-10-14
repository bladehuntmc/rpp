package net.bladehunt.rpp.processor.codegen

import net.bladehunt.rpp.output.BuildContext
import java.io.File

@JvmSynthetic
inline fun CodeGenerator(
    priority: Int = 0,
    crossinline process: (context: BuildContext, config: CodegenConfig, outputDir: File) -> Unit
): CodeGenerator = object : CodeGenerator(priority) {
    override fun generate(context: BuildContext, config: CodegenConfig, outputDir: File) =
        process(context, config, outputDir)
}

abstract class CodeGenerator(val priority: Int) {
    /**
     * @param context The current task's BuildContext
     * @param config The currently loaded codegen configuration
     * @param outputDir The code generation output directory
     */
    abstract fun generate(context: BuildContext, config: CodegenConfig, outputDir: File)
}