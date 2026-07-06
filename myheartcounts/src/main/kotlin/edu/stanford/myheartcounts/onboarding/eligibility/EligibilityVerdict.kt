package edu.stanford.myheartcounts.onboarding.eligibility

import edu.stanford.myheartcounts.model.Country

/**
 * The outcome of evaluating the eligibility answers: [Eligible], a general [Ineligible], or an
 * otherwise-eligible participant whose country is [CountryComingSoon] or [CountryUnsupported].
 */
sealed interface EligibilityVerdict {
    data object Eligible : EligibilityVerdict
    data object Ineligible : EligibilityVerdict
    data class CountryComingSoon(val country: Country) : EligibilityVerdict
    data class CountryUnsupported(val country: Country) : EligibilityVerdict
}
