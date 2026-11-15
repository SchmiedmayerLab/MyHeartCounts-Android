//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayout
import edu.stanford.myheartcounts.onboarding.eligibility.CountrySelectionSheet
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayout
import edu.stanford.myheartcounts.onboarding.eligibility.EmailField
import edu.stanford.myheartcounts.onboarding.eligibility.IneligibleContent
import edu.stanford.myheartcounts.onboarding.eligibility.LinkRow
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.TextRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.onboarding.OnboardingActions
import org.grovealliance.onboarding.OnboardingInformation
import org.grovealliance.onboarding.OnboardingLayout
import org.grovealliance.onboarding.OnboardingTitle
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSectionsBuilderScope
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.Colors
import org.junit.Test

class MyHeartCountsOnboardingScreenshotTest : ScreenshotTest() {

    @Test
    fun `LearnMoreSheet screenshot`() {
        screenshot {
            LearnMoreSheet(
                appBar = groveAppBar {
                    title("Study Overview")
                    close { }
                },
                description = StringResource(MHCStrings.onboarding_study_overview_learn_more),
            ).Sheet(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `OnboardingLayout ineligible screenshot`() {
        screenshot {
            Scaffold {
                OnboardingLayout(
                    header = OnboardingTitle(
                        title = StringResource(MHCStrings.eligibility_ineligible_title),
                        subtitle = StringResource(MHCStrings.eligibility_ineligible_subtitle),
                        customSubtitle = IneligibleContent(
                            email = null,
                            actionButton = null,
                            link = studyWebsiteLink(),
                        ),
                    ),
                    content = OnboardingInformation(areas = emptyList()),
                    wrapInScrollView = false,
                    footer = null,
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `OnboardingLayout reactivate account screenshot`() {
        screenshot {
            Scaffold {
                OnboardingLayout(
                    header = OnboardingTitle(
                        icon = ImageResource(image = Icons.Outlined.AccountCircle),
                        title = StringResource(MHCStrings.onboarding_reactivate_title),
                        subtitle = StringResource(MHCStrings.onboarding_reactivate_subtitle),
                    ),
                    content = OnboardingInformation(areas = emptyList()),
                    footer = OnboardingActions(
                        primaryButton = AsyncTextButton(
                            title = StringResource(MHCStrings.onboarding_reactivate_confirm),
                            action = {},
                        ),
                        secondaryButton = AsyncTextButton(
                            title = StringResource(MHCStrings.onboarding_reactivate_decline),
                            containerColor = { Colors.transparent },
                            textColor = { Colors.primary },
                            action = {},
                        ),
                    ),
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `OnboardingLayout country unavailable screenshot`() {
        screenshot {
            Scaffold {
                OnboardingLayout(
                    header = OnboardingTitle(
                        icon = ImageResource(image = Icons.Outlined.Language),
                        title = StringResource(MHCStrings.eligibility_region_unsupported_title),
                        subtitle = StringResource(
                            MHCStrings.eligibility_region_unavailable_description,
                            "Germany",
                        ),
                        customSubtitle = IneligibleContent(
                            email = EmailField(
                                value = MutableStateFlow("participant@example.com"),
                                placeholder = StringResource(MHCStrings.eligibility_region_notify_email_hint),
                                onValueChange = { },
                            ),
                            actionButton = AsyncTextButton(
                                title = StringResource(MHCStrings.eligibility_region_notify_button),
                                action = { },
                            ),
                            link = studyWebsiteLink(),
                        ),
                    ),
                    content = OnboardingInformation(areas = emptyList()),
                    footer = null,
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `EligibilityLayout screenshot`() {
        screenshot {
            Scaffold(appBarTitle = "Eligibility") {
                eligibilityLayout().Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `ConsentSurveyLayout screenshot`() {
        screenshot {
            Scaffold(appBarTitle = "Consent Survey") {
                consentSurveyLayout().Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `CountrySelectionSheet screenshot`() {
        screenshot {
            countrySelectionSheet().Sheet(modifier = Modifier.fillMaxSize())
        }
    }

    private fun eligibilityLayout() = EligibilityLayout(
        sections = MutableStateFlow(
            groveSectionList {
                rowSection(
                    key = "description",
                    title = null,
                    content = TextRow(text = StringResource(MHCStrings.eligibility_subtitle)),
                )
                rowSection(
                    key = "age",
                    title = MHCStrings.eligibility_age_title,
                    content = yesNoRow(
                        text = MHCStrings.eligibility_age_question,
                        subtext = null,
                        value = true,
                        style = BooleanOptionRow.Style.Switch,
                    ),
                )
                rowSection(
                    key = "region",
                    title = MHCStrings.eligibility_region_title,
                    content = LinkRow(
                        text = StringResource(MHCStrings.eligibility_region_question),
                        valueLabel = MutableStateFlow(StringResource("Germany")),
                        onClicked = { },
                    ),
                )
                rowSection(
                    key = "language",
                    title = MHCStrings.eligibility_language_title,
                    content = yesNoRow(
                        text = MHCStrings.eligibility_language_question,
                        subtext = null,
                        value = true,
                        style = BooleanOptionRow.Style.Label,
                    ),
                )
                rowSection(
                    key = "account-sharing",
                    title = MHCStrings.eligibility_account_sharing_title,
                    content = yesNoRow(
                        text = MHCStrings.eligibility_account_sharing_question,
                        subtext = MHCStrings.eligibility_account_sharing_explanation,
                        value = false,
                        style = BooleanOptionRow.Style.Label,
                    ),
                )
                buttonSection(
                    key = "save",
                    title = MHCStrings.eligibility_save,
                )
            },
        ),
    )

    private fun consentSurveyLayout() = ConsentSurveyLayout(
        sections = MutableStateFlow(
            groveSectionList {
                rowSection(
                    key = "header",
                    title = null,
                    content = TextRow(text = StringResource(MHCStrings.consent_survey_header)),
                )
                rowSection(
                    key = "seek-help",
                    title = null,
                    content = trueFalseRow(
                        text = MHCStrings.consent_survey_question_seek_help,
                        value = true,
                    ),
                )
                rowSection(
                    key = "voluntary",
                    title = null,
                    content = trueFalseRow(
                        text = MHCStrings.consent_survey_question_voluntary,
                        value = null,
                    ),
                )
                rowSection(
                    key = "withdraw",
                    title = null,
                    content = trueFalseRow(
                        text = MHCStrings.consent_survey_question_withdraw,
                        value = false,
                    ),
                )
                buttonSection(
                    key = "continue",
                    title = MHCStrings.onboarding_continue,
                )
            },
        ),
    )

    private fun countrySelectionSheet() = CountrySelectionSheet(
        rows = listOf(
            CountrySelectionSheet.CountryRow(
                label = "🇺🇸  United States",
                selected = true,
                onSelect = { },
                matches = { true },
            ),
            CountrySelectionSheet.CountryRow(
                label = "🇮🇹  Italy",
                selected = false,
                onSelect = { },
                matches = { true },
            ),
            CountrySelectionSheet.CountryRow(
                label = "🇩🇪  Germany",
                selected = false,
                onSelect = { },
                matches = { true },
            ),
        ),
        onClose = { },
    )

    private fun studyWebsiteLink() = ExternalLink(
        text = StringResource(MHCStrings.eligibility_learn_more),
        onClicked = { },
    )

    @Composable
    private fun Scaffold(appBarTitle: String? = null, content: @Composable BoxScope.() -> Unit) {
        StaticGroveScaffold(
            appBar = groveAppBar {
                appBarTitle?.let { title(it) }
                back { }
            },
            content = content,
        )
    }

    private fun yesNoRow(
        @StringRes text: Int,
        @StringRes subtext: Int?,
        value: Boolean,
        style: BooleanOptionRow.Style,
    ) = BooleanOptionRow(
        text = StringResource(text),
        subtext = subtext?.let { StringResource(it) },
        value = MutableStateFlow(value),
        onChange = { },
        style = style,
        confirmLabel = StringResource(MHCStrings.answer_yes),
        declineLabel = StringResource(MHCStrings.answer_no),
    )

    private fun trueFalseRow(
        @StringRes text: Int,
        value: Boolean?,
    ) = BooleanOptionRow(
        text = StringResource(text),
        subtext = null,
        value = MutableStateFlow(value),
        onChange = { },
        style = BooleanOptionRow.Style.Label,
        confirmLabel = StringResource(MHCStrings.answer_true),
        declineLabel = StringResource(MHCStrings.answer_false),
    )

    private fun GroveSectionsBuilderScope.rowSection(
        key: String,
        @StringRes title: Int?,
        content: ComposableContent,
    ) {
        section(key = key) {
            title?.let { title(resId = it) }
            card(key = key) { row(key = key, content = content) }
        }
    }

    private fun GroveSectionsBuilderScope.buttonSection(
        key: String,
        @StringRes title: Int,
    ) {
        section(key = key) {
            content(
                key = key,
                content = AsyncTextButton(
                    title = StringResource(title),
                    action = { },
                ),
            )
        }
    }
}
