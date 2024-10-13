package net.bladehunt.rpp.processor.codegen

data class CodegenProcessorBuilder(
    var priority: Int = 1000,
    val generators: MutableList<CodeGenerator> = arrayListOf(),
) {
    internal fun build(): CodegenOutputProcessor = CodegenOutputProcessor(priority, generators)
}