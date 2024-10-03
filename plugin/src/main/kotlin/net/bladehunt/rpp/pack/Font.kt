package net.bladehunt.rpp.pack

data class Font(
    val namespace: String,
    val font: String
) {
    override fun toString(): String = "$namespace:$font"
}
