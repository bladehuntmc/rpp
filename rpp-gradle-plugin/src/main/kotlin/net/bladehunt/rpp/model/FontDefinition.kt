package net.bladehunt.rpp.model

import kotlinx.serialization.Serializable

@Serializable
internal data class FontDefinition(
    val providers: List<FontProvider>
)