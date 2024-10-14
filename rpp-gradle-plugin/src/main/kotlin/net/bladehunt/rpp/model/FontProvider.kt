package net.bladehunt.rpp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface FontProvider {
    val filter: Filter?

    @Serializable
    @SerialName("bitmap")
    data class Bitmap(
        val file: String,
        val name: String? = null,
        val height: Int? = null,
        val ascent: Int,
        val chars: List<String>,
        override val filter: Filter? = null
    ) : FontProvider

    @Serializable
    @SerialName("space")
    data class Space(
        val advances: Map<String, Float>,
        override val filter: Filter? = null
    ) : FontProvider

    @Serializable
    @SerialName("legacy_unicode")
    data class LegacyUnicode(
        val sizes: String,
        val template: String,
        override val filter: Filter? = null
    ) : FontProvider

    @Serializable
    @SerialName("ttf")
    data class Ttf(
        val file: String,
        val shift: List<Float>,
        val size: Float,
        val oversample: Float,
        override val filter: Filter? = null
    ) : FontProvider

    @Serializable
    @SerialName("unihex")
    data class Unihex(
        @SerialName("hex_file")
        val hexFile: String,
        @SerialName("size_overrides")
        val sizeOverrides: List<SizeOverride>,
        override val filter: Filter? = null
    ) : FontProvider {
        @Serializable
        data class SizeOverride(
            val from: String,
            val to: String,
            val left: Int,
            val right: Int
        )
    }

    @Serializable
    @SerialName("reference")
    data class Reference(
        val id: String,
        override val filter: Filter?
    ) : FontProvider

    @Serializable
    data class Filter(
        val uniform: Boolean,
        val jp: Boolean
    )
}