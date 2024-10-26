package net.bladehunt.rpp.processor

import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.api.Json
import net.bladehunt.rpp.build.BuildScope
import net.bladehunt.rpp.build.ResourcePackProcessor
import net.bladehunt.rpp.model.FontDefinition
import net.bladehunt.rpp.model.FontProvider
import net.bladehunt.rpp.model.Resource
import net.bladehunt.rpp.util.java
import net.bladehunt.rpp.util.readJsonOrNull
import kotlin.math.pow

data class SpacesProcessor(
    val resource: Resource,
    val charCount: Int,
    val `package`: String?,
    val className: String?,
) : PostProcessor<Nothing?> {

    override fun BuildScope.process() {
        val fontDir = rpp.layout.build.output.resolve("assets/${resource.namespace}/font")
        fontDir.mkdirs()

        val fontFile = fontDir.resolve("${resource.value}.json")

        val current =
            if (fontFile.exists()) fontFile.readJsonOrNull<FontDefinition>()
            else null

        fontFile.createNewFile()

        fontFile.outputStream().use { out ->
            val provider = FontProvider.Space(
                buildMap {
                    for (i in 0..<charCount) {
                        put(Char(57344 + i).toString(), 2.0.pow(i).toFloat())
                        put(Char(61440 + i).toString(), -2.0.pow(i).toFloat())
                    }
                }
            )

            if (current != null) {
                Json.encodeToStream(FontDefinition(null, listOf(provider)), out)
                current.copy(
                    providers = buildList {
                        current.providers.forEach { provider ->
                            if (provider !is FontProvider.Space || provider.generated == null)
                            add(provider)
                        }
                        add(provider)
                    }
                )
                return@use
            }

            Json.encodeToStream(FontDefinition(null, listOf(provider)), out)
        }

        if (`package` == null) return

        requireNotNull(className) { "To generate code, the package and className must not be null" }

        val packageDir = rpp.layout.build.generated.java.resolve(`package`.replace('.', '/'))

        packageDir.mkdirs()

        val classFile = packageDir.resolve("$className.java")

        classFile.createNewFile()

        classFile.writeText(
            java(
                """
                package $`package`;
        
                import net.kyori.adventure.key.Key;
                import net.kyori.adventure.text.Component;
        
                import java.util.LinkedHashMap;
                import java.lang.Math;
        
                public class $className {
                    public static Key FONT_KEY = Key.key("${resource.namespace}", "${resource.value}");
                
                    private static char charFor(int n) {
                        return (char) ((n < 0 ? 61440 : 57344) + intSqrt(n));
                    }
                    
                    public static Component component(int amount) {
                        return Component.text(string(amount)).font(FONT_KEY);
                    }
        
                    public static String string(int amount) {
                        StringBuilder builder = new StringBuilder();
                        
                        int modifier = amount < 0 ? -1 : 1;
                        
                        amount = Math.abs(amount);
                
                        while (true) {
                            int highestAmount = Math.min(Integer.highestOneBit(amount), $charCount);
                            if (highestAmount == 0) return builder.toString();
                            
                            builder.append(charFor(highestAmount * modifier));
                            amount -= highestAmount;
                        }
                    }
            
                    private static int intSqrt(int n) {
                        n = Math.abs(n);
        
                        if ((n & (n - 1)) != 0 || n > $charCount) {
                            throw new IllegalArgumentException("Input must be a power of 2 and the absolute value must not be greater than $charCount.");
                        }
        
                        return Integer.numberOfTrailingZeros(n);
                    }
                }""".trimIndent()
            )
        )
    }

    override fun createContext(rpp: ResourcePackProcessor): Nothing? = null
}

fun RppExtension.generateSpaces(
    resource: Resource,
    charCount: Int,
    `package`: String? = null,
    className: String? = null
): SpacesProcessor =
    SpacesProcessor(resource, charCount, `package`, className).also {
        if (`package` == null) requireNotNull(className) { "To generate code, the package and className must not be null" }
        outputProcessors += it
    }