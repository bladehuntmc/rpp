package net.bladehunt.rpp.codegen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.rpp.util.java
import java.io.Writer

@Serializable
internal sealed interface FontProvider {
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

@Serializable
internal data class FontDefinition(
    val providers: List<FontProvider>
)

internal fun generateFontClass(
    `package`: String,
    className: String,
    namespace: String,
    font: String,
    definition: FontDefinition,
    spacePrefix: String,
    out: Writer
) {
    out.write(
        java(
            """
                package $`package`;
                
                import net.kyori.adventure.key.Key;
                import net.kyori.adventure.text.Component;
                
                // Generated by https://github.com/bladehuntmc/rpp
                public class $className {
                    public static final Key FONT_KEY = Key.key("$namespace", "$font");
            """.trimIndent()
        )
    )

    out.write("\n")

    var withoutName = 0

    definition.providers.forEach { provider ->
        when (provider) {
            is FontProvider.Bitmap -> {
                val name = provider.name ?: run {
                    withoutName++
                    return@forEach
                }

                val firstChar = provider.chars.firstOrNull() ?: return@forEach

                out.write(java("\n    public static final Component $name = Component.text(\"$firstChar\").font(FONT_KEY);"))
            }
            is FontProvider.Space -> {
                provider.advances.forEach { (key, advance) ->
                    out.write(java("\n    public static final Component ${spacePrefix}_${advance
                        .toString()
                        .replace('.', '_')
                        .replace("-","NEG_")
                        .removeSuffix("_0")
                    } = Component.text(\"$key\").font(FONT_KEY);"))
                }
            }
            // TODO: Implement other providers
            else -> {}
        }
    }

    if (withoutName > 0) println("Warn: $withoutName provider(s) in $font does not have a name")

    out.write("\n")
    out.write("}")
}