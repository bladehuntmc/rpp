package net.bladehunt.rpp.codegen

import net.bladehunt.rpp.util.java
import kotlin.math.pow

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

internal fun createSpaceClass(
    className: String,
    classPackage: String,
    namespace: String,
    font: String,
    amount: Int
): String {
    val amt = 2.0.pow(amount-1).toInt()
    return javaFile
        .replace("rpp_amount", amt.toString())
        .replace("rpp_pkg", classPackage)
        .replace("rpp_namespace", namespace)
        .replace("rpp_font", font)
        .replace("rpp_name", className)
}