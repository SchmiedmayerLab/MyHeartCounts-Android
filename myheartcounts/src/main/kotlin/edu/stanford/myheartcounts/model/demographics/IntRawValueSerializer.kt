package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes an enum by a stable [Int] raw value, falling back to [default] for unknown values
 * rather than throwing.
 */
class IntRawValueSerializer<T>(
    serialName: String,
    private val entries: List<T>,
    private val rawValue: (T) -> Int,
    private val default: T,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(rawValue(value))
    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeInt()
        return entries.firstOrNull { rawValue(it) == raw } ?: default
    }
}
