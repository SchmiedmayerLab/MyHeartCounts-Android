package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A participant's selected comorbidities: a map of comorbidity id to an optional start-date string
 * (`yyyy-MM`, `yyyy`, or empty).
 */
@Serializable(with = Comorbidities.Serializer::class)
data class Comorbidities(val entries: Map<String, String>) {

    val isEmpty: Boolean get() = entries.isEmpty()
    val count: Int get() = entries.size

    object Serializer : KSerializer<Comorbidities> {
        private val delegate = MapSerializer(String.serializer(), String.serializer())
        override val descriptor = delegate.descriptor
        override fun serialize(encoder: Encoder, value: Comorbidities) = delegate.serialize(encoder, value.entries)
        override fun deserialize(decoder: Decoder): Comorbidities = Comorbidities(delegate.deserialize(decoder))
    }
}
