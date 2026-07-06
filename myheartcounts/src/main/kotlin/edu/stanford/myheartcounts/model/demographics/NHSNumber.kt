package edu.stanford.myheartcounts.model.demographics

import edu.stanford.myheartcounts.model.demographics.NHSNumber.Companion.validate
import kotlinx.serialization.Serializable

/**
 * A 10-digit NHS number.
 */
@Serializable
@JvmInline
value class NHSNumber(val stringValue: String) {

    companion object {
        private const val LENGTH = 10
        private const val MODULUS = 11

        /**
         * Creates an [NHSNumber] from [input] (spaces/dashes stripped) only if it passes [validate].
         */
        fun validating(input: String): NHSNumber? {
            val normalized = input.filterNot { it == ' ' || it == '-' }
            return if (validate(normalized)) NHSNumber(normalized) else null
        }

        /**
         * Validates the NHS number checksum (the 10th digit is the check digit of the first 9).
         */
        fun validate(input: String): Boolean {
            val normalized = input.filterNot { it == ' ' || it == '-' }
            val checksum = checksum(normalized) ?: return false
            val lastDigit = normalized.lastOrNull()?.digitToIntOrNull() ?: return false
            return checksum == lastDigit
        }

        private fun checksum(input: String): Int? {
            if (input.length != LENGTH) return null
            var sum = 0
            input.take(LENGTH - 1).forEachIndexed { index, char ->
                val digit = char.digitToIntOrNull() ?: return null
                sum += digit * (MODULUS - (index + 1))
            }
            val remainder = MODULUS - (sum % MODULUS)
            return when (remainder) {
                MODULUS -> 0
                MODULUS - 1 -> null
                else -> remainder
            }
        }
    }
}
