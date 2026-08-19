//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:CyclomaticComplexMethod")

package edu.stanford.myheartcounts.onboarding

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapper
import edu.stanford.myheartcounts.onboarding.eligibility.CountrySelectionSheet
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapper
import edu.stanford.myheartcounts.onboarding.eligibility.EmailField
import edu.stanford.myheartcounts.onboarding.eligibility.IneligibleContent
import org.grovealliance.onboarding.OnboardingActions
import org.grovealliance.onboarding.OnboardingArea
import org.grovealliance.onboarding.OnboardingInformation
import org.grovealliance.onboarding.OnboardingLayout
import org.grovealliance.onboarding.OnboardingTitle
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors

/**
 * Builds the [ComposableContent] for an [OnboardingStep]. Button interactions are dispatched to
 * the supplied [ActionSink] instead of being handled inline, keeping the produced layout free of
 * navigation and presentation concerns.
 */
interface OnboardingStepLayoutMapper {
    fun map(input: OnboardingStepInput): ComposableContent

    /**
     * Builds the country picker from the countries supplied in [input]. Selection and dismissal are
     * dispatched to the input's [ActionSink].
     */
    fun countrySelectionSheet(input: OnboardingStepInput): CountrySelectionSheet
}

/**
 * Default [OnboardingStepLayoutMapper] implementation.
 */
class OnboardingStepLayoutMapperImpl(
    private val eligibilityLayoutMapper: EligibilityLayoutMapper,
    private val consentSurveyLayoutMapper: ConsentSurveyLayoutMapper,
) : OnboardingStepLayoutMapper {

    override fun map(input: OnboardingStepInput): ComposableContent = when (input.step) {
        OnboardingStep.Welcome -> welcome(input)
        OnboardingStep.Eligibility -> eligibilityLayoutMapper.map(input)
        OnboardingStep.Ineligible -> ineligible(input)
        OnboardingStep.CountryUnavailable -> countryUnavailable(input)
        OnboardingStep.StudyOverview -> infoStep(
            input = input,
            icon = Icons.Outlined.AssignmentTurnedIn,
            title = MHCStrings.onboarding_study_overview_title,
            subtitle = MHCStrings.onboarding_study_overview_subtitle,
            learnMore = MHCStrings.onboarding_study_overview_learn_more,
        )

        OnboardingStep.Login -> LoginStep(
            onSuccess = { input.push(OnboardingAction.Next) },
            onDismiss = { input.push(OnboardingAction.Previous) },
        )

        OnboardingStep.TrialComponent -> infoStep(
            input = input,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            title = MHCStrings.onboarding_trial_component_title,
            subtitle = MHCStrings.onboarding_trial_component_subtitle,
            learnMore = MHCStrings.onboarding_trial_component_learn_more,
        )

        OnboardingStep.DataCollection -> infoStep(
            input = input,
            icon = Icons.Outlined.Security,
            title = MHCStrings.onboarding_data_collection_title,
            subtitle = MHCStrings.onboarding_data_collection_subtitle,
            learnMore = MHCStrings.onboarding_data_collection_learn_more,
        )

        OnboardingStep.RisksAndBenefits -> infoStep(
            input = input,
            icon = Icons.Outlined.Balance,
            title = MHCStrings.onboarding_risks_benefits_title,
            subtitle = MHCStrings.onboarding_risks_benefits_subtitle,
            learnMore = MHCStrings.onboarding_risks_benefits_learn_more,
        )

        OnboardingStep.Comprehension -> consentSurveyLayoutMapper.map(input)
        OnboardingStep.Consent -> ConsentStep(
            onConsent = { responses ->
                input.push(
                    OnboardingAction.OnboardingAnswersChanged(
                        input.answers.value.copy(consentResponses = responses),
                    ),
                )
                input.advance(input.step)
            },
        )

        OnboardingStep.HealthAccess -> infoStep(
            input = input,
            icon = Icons.Outlined.HealthAndSafety,
            title = MHCStrings.onboarding_health_access_title,
            subtitle = MHCStrings.onboarding_health_access_subtitle,
            learnMore = MHCStrings.onboarding_health_access_learn_more,
        )

        OnboardingStep.Notifications -> notificationStep(input = input)
        OnboardingStep.FinalEnrollment -> finalEnrollment(input)
    }

    private fun welcome(input: OnboardingStepInput) = OnboardingLayout(
        header = OnboardingTitle(
            title = StringResource(MHCStrings.onboarding_welcome_title),
            subtitle = StringResource(MHCStrings.onboarding_welcome_subtitle),
        ),
        content = OnboardingInformation(
            areas = listOf(
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.Lightbulb),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_welcome_area_involvement),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.AutoAwesome),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_welcome_area_redesign),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.Watch),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_welcome_area_online),
                ),
            ),
        ),
        footer = OnboardingActions(
            primaryButton = AsyncTextButton(
                title = StringResource(MHCStrings.onboarding_continue),
                action = { input.push(OnboardingAction.Next) },
            ),
        ),
    )

    private fun ineligible(input: OnboardingStepInput) = OnboardingLayout(
        header = OnboardingTitle(
            title = StringResource(MHCStrings.eligibility_ineligible_title),
            subtitle = StringResource(MHCStrings.eligibility_ineligible_subtitle),
            customSubtitle = IneligibleContent(
                email = null,
                actionButton = null,
                link = studyWebsiteLink(input),
            ),
        ),
        content = OnboardingInformation(areas = emptyList()),
        wrapInScrollView = false,
        footer = null,
    )

    private fun countryUnavailable(input: OnboardingStepInput): ComposableContent {
        val country = input.answers.value.country
        val title = if (country?.isComingSoon == true) {
            MHCStrings.eligibility_region_coming_soon_title
        } else {
            MHCStrings.eligibility_region_unsupported_title
        }
        return OnboardingLayout(
            header = OnboardingTitle(
                icon = ImageResource(image = Icons.Outlined.Language),
                title = StringResource(title),
                subtitle = StringResource(
                    MHCStrings.eligibility_region_unavailable_description,
                    country?.name.orEmpty(),
                ),
                customSubtitle = IneligibleContent(
                    email = EmailField(
                        value = input.waitlistEmail,
                        placeholder = StringResource(MHCStrings.eligibility_region_notify_email_hint),
                        onValueChange = { input.push(OnboardingAction.WaitlistEmailChanged(it)) },
                    ),
                    actionButton = AsyncTextButton(
                        title = StringResource(MHCStrings.eligibility_region_notify_button),
                        action = { input.advance(input.step) },
                    ),
                    link = studyWebsiteLink(input),
                ),
            ),
            content = OnboardingInformation(areas = emptyList()),
            footer = null,
        )
    }

    private fun studyWebsiteLink(input: OnboardingStepInput) = OnboardingLink(
        text = StringResource(MHCStrings.eligibility_learn_more),
        onClicked = { input.push(OnboardingAction.OpenStudyWebsite) },
    )

    private fun finalEnrollment(input: OnboardingStepInput) = OnboardingLayout(
        header = OnboardingTitle(
            title = StringResource(MHCStrings.onboarding_final_enrollment_title),
            subtitle = StringResource(MHCStrings.onboarding_final_enrollment_subtitle),
        ),
        content = OnboardingInformation(
            areas = listOf(
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.CalendarMonth),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_final_enrollment_area_activities),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.Timer),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_final_enrollment_area_tasks),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.MonitorHeart),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_final_enrollment_area_collection),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.AutoMirrored.Outlined.CompareArrows),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_final_enrollment_area_trial),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Outlined.Favorite),
                    title = null,
                    description = StringResource(MHCStrings.onboarding_final_enrollment_area_contribution),
                ),
            ),
        ),
        footer = OnboardingActions(
            primaryButton = AsyncTextButton(
                title = StringResource(MHCStrings.onboarding_start),
                coroutineScope = { input.scope },
                action = { input.advance(input.step) },
            ),
        ),
    )

    private fun infoStep(
        input: OnboardingStepInput,
        icon: ImageVector,
        @StringRes title: Int,
        @StringRes subtitle: Int,
        @StringRes learnMore: Int? = null,
        @StringRes primaryText: Int = MHCStrings.onboarding_continue,
    ): OnboardingLayout = OnboardingLayout(
        header = OnboardingTitle(
            icon = ImageResource(image = icon),
            title = StringResource(title),
            subtitle = StringResource(subtitle),
        ),
        content = OnboardingInformation(areas = emptyList()),
        footer = OnboardingActions(
            primaryButton = AsyncTextButton(
                title = StringResource(primaryText),
                coroutineScope = { input.scope },
                action = { input.advance(input.step) },
            ),
            secondaryButton = learnMore?.let { content ->
                AsyncTextButton(
                    title = StringResource(MHCStrings.onboarding_learn_more),
                    containerColor = { Colors.transparent },
                    textColor = { Colors.primary },
                    action = {
                        input.push(
                            OnboardingAction.ShowLearnMore(title = StringResource(title), content = StringResource(content)),
                        )
                    },
                )
            },
        ),
    )

    private fun notificationStep(
        input: OnboardingStepInput,
    ): OnboardingLayout = OnboardingLayout(
        header = OnboardingTitle(
            icon = ImageResource(image = Icons.Outlined.Notifications),
            title = StringResource(MHCStrings.onboarding_notifications_title),
            subtitle = StringResource(MHCStrings.onboarding_notifications_subtitle),
        ),
        content = OnboardingInformation(areas = emptyList()),
        footer = OnboardingActions(
            primaryButton = AsyncTextButton(
                title = StringResource(MHCStrings.onboarding_notifications_title),
                coroutineScope = { input.scope },
                action = { input.push(action = OnboardingAction.NotificationPermissionRequested) },
            ),
            secondaryButton = AsyncTextButton(
                title = StringResource(MHCStrings.onboarding_skip),
                containerColor = { Colors.transparent },
                textColor = { Colors.primary },
                action = { input.push(OnboardingAction.NotificationSkipped) },
            )
        ),
    )

    override fun countrySelectionSheet(input: OnboardingStepInput): CountrySelectionSheet {
        val selectedCode = input.answers.value.country?.code
        val rows = input.countries.map { country ->
            val flagEmoji = flagEmoji(country.code)
            CountrySelectionSheet.CountryRow(
                label = if (flagEmoji != null) "$flagEmoji  ${country.name}" else country.name,
                selected = country.code == selectedCode,
                onSelect = { input.push(OnboardingAction.CountriesSheetDismissed(country)) },
                matches = { query ->
                    country.name.contains(query, ignoreCase = true) || country.code.contains(query, ignoreCase = true)
                },
            )
        }
        return CountrySelectionSheet(
            rows = rows,
            onClose = { input.push(OnboardingAction.CountriesSheetDismissed(selectedCountry = null)) },
        )
    }

    /**
     * Returns the flag emoji for an ISO 3166-1 alpha-2 country [code], or an empty string when
     * [code] is not two ASCII letters.
     */
    private fun flagEmoji(code: String): String? {
        if (code.length != ISO_ALPHA2_LENGTH || code.any { !it.isLetter() }) return null
        return code.uppercase()
            .map { letter -> REGIONAL_INDICATOR_OFFSET + letter.code }
            .joinToString(separator = "") { codePoint -> String(Character.toChars(codePoint)) }
    }

    companion object {
        // 0x1F1E6 (regional indicator 'A') minus 'A' (0x41) = 127397.
        private const val REGIONAL_INDICATOR_OFFSET = 127397
        private const val ISO_ALPHA2_LENGTH = 2
    }
}
