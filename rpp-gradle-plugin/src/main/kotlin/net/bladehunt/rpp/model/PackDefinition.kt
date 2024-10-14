package net.bladehunt.rpp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackDefinition(
    val pack: Pack,
    val lang: Pack? = null,
    val filter: Filter? = null
) {
    @Serializable
    data class Pack(
        @SerialName("pack_format")
        val packFormat: Int,
        val description: Map<Resource, Lang>
    )

    @Serializable
    data class Lang(
        val name: String,
        val region: String,
        val bidirectional: Boolean = false
    )

    @Serializable
    data class Filter(
        val block: List<Pattern> = emptyList()
    ) {
        @Serializable
        data class Pattern(
            val namespace: String? = null,
            val path: String? = null
        )
    }
}