package net.bladehunt.rpp.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun Resource(value: String): Resource {
    val split = value.split(':', limit = 2)
    return when (split.size) {
        1 -> Resource("minecraft", value)
        2 -> Resource(split[0], split[1])
        else -> {
            throw AssertionError("This should never happen")
        }
    }
}

@Serializable(with = ResourceSerializer::class)
data class Resource(val namespace: String, val value: String) {
    override fun toString(): String = "$namespace:$value"
}

object ResourceSerializer : KSerializer<Resource> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(Resource::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Resource = Resource(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Resource) = encoder.encodeString(value.toString())
}