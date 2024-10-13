package net.bladehunt.rpp.processor.codegen

import net.bladehunt.rpp.output.BuildContext
import net.bladehunt.rpp.processor.output.SpacesProcessor
import net.bladehunt.rpp.util.java
import java.io.File

private val javaFile = java(
    """
        package rpp_pkg;

        import net.kyori.adventure.key.Key;
        import net.kyori.adventure.text.Component;

        import java.util.LinkedHashMap;
        import java.lang.Math;

        public class rpp_name {
            public static Key FONT_KEY = Key.key("rpp_namespace", "rpp_font");
        
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
                    int highestAmount = Math.min(Integer.highestOneBit(amount), rpp_amount);
                    if (highestAmount == 0) return builder.toString();
                    
                    builder.append(charFor(highestAmount * modifier));
                    amount -= highestAmount;
                }
            }
    
            private static int intSqrt(int n) {
                n = Math.abs(n);

                if ((n & (n - 1)) != 0 || n > rpp_amount) {
                    throw new IllegalArgumentException("Input must be a power of 2 and the absolute value must not be greater than rpp_amount.");
                }

                return Integer.numberOfTrailingZeros(n);
            }
        }
    """.trimIndent()
)

object SpacesGenerator : CodeGenerator(Int.MIN_VALUE) {
    override fun generate(context: BuildContext, config: CodegenConfig, outputDir: File) {
        val processors = context.outputProcessors.filterIsInstance<SpacesProcessor>()
        config.spaces.forEach { spaceConfig ->
            val processor = processors.firstOrNull { spaceConfig.font == it.font }

            if (processor == null) {
                context.logger.warn("The codegen configuration contained a nonexistent space font")
                return@forEach
            }

            val pkg = spaceConfig.packageOverride ?:  (config.basePackage + ".spaces")
            val outputPackage = outputDir.resolve(pkg.replace('.', '/'))

            outputPackage.mkdirs()

            val output = outputPackage.resolve("${spaceConfig.className}.java")

            output.writeText(
                javaFile
                    .replace("rpp_amount", processor.amount.toString())
                    .replace("rpp_pkg", pkg)
                    .replace("rpp_namespace", spaceConfig.font.namespace)
                    .replace("rpp_font", spaceConfig.font.value)
                    .replace("rpp_name", spaceConfig.className)
            )
        }
    }
}