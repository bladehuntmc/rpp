package net.bladehunt.rpp.spaces

import kotlinx.serialization.json.encodeToStream
import net.bladehunt.rpp.Json
import net.bladehunt.rpp.codegen.FontDefinition
import net.bladehunt.rpp.codegen.FontProvider
import java.io.OutputStream
import kotlin.math.pow

internal fun writeSpaces(amount: Int, stream: OutputStream) = Json.encodeToStream(
    FontDefinition(
        listOf(
            FontProvider.Space(
                buildMap {
                    for (i in 0..<amount) {
                        put(Char(57344 + i).toString(), 2.0.pow(i).toFloat())
                        put(Char(61440 + i).toString(), -2.0.pow(i).toFloat())
                    }
                }
            )
        )
    ),
    stream
)

