package net.bladehunt.rpp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FontDefinition(
    val codegen: Codegen? = null,
    val providers: List<FontProvider>
) {
    @Serializable
    data class Codegen(
        @SerialName("package")
        val packageQualifier: String,
        val className: String
    )
}