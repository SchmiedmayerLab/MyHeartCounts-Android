@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.Serializable

/**
 * A participant's race / ethnicity — a set of options held as a bitmask. Combine options with [plus]
 * and test membership with [contains].
 */
@Serializable
@JvmInline
value class RaceEthnicity(val rawValue: Long) {

    val isEmpty: Boolean get() = rawValue == 0L

    operator fun contains(option: RaceEthnicity): Boolean = (rawValue and option.rawValue) == option.rawValue

    operator fun plus(option: RaceEthnicity): RaceEthnicity = RaceEthnicity(rawValue or option.rawValue)

    companion object {
        val NONE = RaceEthnicity(0L)
        val PREFER_NOT_TO_STATE = RaceEthnicity(1L shl 0)
        val WHITE = RaceEthnicity(1L shl 1)
        val BLACK = RaceEthnicity(1L shl 2)
        val AMERICAN_INDIAN = RaceEthnicity(1L shl 3)
        val ALASKA_NATIVE = RaceEthnicity(1L shl 4)
        val ASIAN_INDIAN = RaceEthnicity(1L shl 5)
        val CHINESE = RaceEthnicity(1L shl 6)
        val FILIPINO = RaceEthnicity(1L shl 7)
        val JAPANESE = RaceEthnicity(1L shl 8)
        val KOREAN = RaceEthnicity(1L shl 9)
        val VIETNAMESE = RaceEthnicity(1L shl 10)
        val PACIFIC_ISLANDER = RaceEthnicity(1L shl 11)
        val OTHER = RaceEthnicity(1L shl 12)
    }
}
