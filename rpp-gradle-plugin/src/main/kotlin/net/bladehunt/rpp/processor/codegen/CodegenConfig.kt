package net.bladehunt.rpp.processor.codegen

import kotlinx.serialization.Serializable
import net.bladehunt.rpp.model.Resource

@Serializable
data class CodegenConfig(
    val basePackage: String,
    val spaces: List<CodegenSpacesConfig> = emptyList(),
    val fonts: List<CodegenFontConfig> = emptyList()
)

@Serializable
data class CodegenSpacesConfig(
    val font: Resource,
    val className: String,
    val packageOverride: String? = null,
)

@Serializable
data class CodegenFontConfig(
    val font: Resource,
    val packageOverride: String? = null,
    val spacePrefix: String = "SPACE",
    val className: String? = null
)