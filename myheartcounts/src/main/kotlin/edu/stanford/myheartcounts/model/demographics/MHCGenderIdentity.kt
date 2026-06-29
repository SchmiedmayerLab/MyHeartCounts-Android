@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A participant's gender identity. Distinct from [edu.stanford.spezi.account.GenderIdentity], which
 * has a different set of cases.
 */
@Serializable(with = MHCGenderIdentity.Serializer::class)
enum class MHCGenderIdentity(val rawValue: Int) {
    PREFER_NOT_TO_STATE(0),
    MALE(1),
    FEMALE(2),
    TRANS_FEMALE(3),
    TRANS_MALE(4),
    OTHER(5),
    ;

    object Serializer : KSerializer<MHCGenderIdentity> by IntRawValueSerializer(
        serialName = "MHCGenderIdentity",
        entries = entries,
        rawValue = { it.rawValue },
        default = PREFER_NOT_TO_STATE,
    )
}
