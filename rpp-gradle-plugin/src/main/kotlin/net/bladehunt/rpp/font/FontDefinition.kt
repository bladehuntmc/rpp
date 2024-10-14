package net.bladehunt.rpp.font

import kotlinx.serialization.Serializable

@Serializable
internal data class FontDefinition(
    val providers: List<FontProvider>
)