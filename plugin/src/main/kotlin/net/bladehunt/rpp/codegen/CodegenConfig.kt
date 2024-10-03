package net.bladehunt.rpp.codegen

import kotlinx.serialization.Serializable

typealias CodegenConfig = Map<String, CodegenNamespaceConfig>

@Serializable
data class CodegenNamespaceConfig(
    val basePackage: String,
    val fonts: Map<String, CodegenFontConfig>? = emptyMap()
)

@Serializable
data class CodegenFontConfig(
    val packageOverride: String? = null,
    val spacePrefix: String = "SPACE",
    val className: String? = null
)