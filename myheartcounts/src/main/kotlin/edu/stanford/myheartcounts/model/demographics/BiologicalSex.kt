@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A participant's biological sex at birth.
 */
@Serializable(with = BiologicalSex.Serializer::class)
enum class BiologicalSex(val rawValue: Int) {
    PREFER_NOT_TO_STATE(0),
    MALE(1),
    FEMALE(2),
    INTERSEX(3),
    ;

    object Serializer : KSerializer<BiologicalSex> by IntRawValueSerializer(
        serialName = "BiologicalSex",
        entries = entries,
        rawValue = { it.rawValue },
        default = PREFER_NOT_TO_STATE,
    )
}
