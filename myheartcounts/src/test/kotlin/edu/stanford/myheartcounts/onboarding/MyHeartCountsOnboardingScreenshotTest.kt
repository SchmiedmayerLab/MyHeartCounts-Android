package edu.stanford.myheartcounts.onboarding

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
import edu.stanford.myheartcounts.onboarding.eligibility.Section
import edu.stanford.myheartcounts.ui.BooleanOptionRow
import edu.stanford.spezi.onboarding.OnboardingInformation
import edu.stanford.spezi.onboarding.OnboardingLayout
import edu.stanford.spezi.onboarding.OnboardingTitle
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StaticSpeziScaffold
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.speziAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class MyHeartCountsOnboardingScreenshotTest : ScreenshotTest() {

    @Test
    fun `LearnMoreSheet screenshot`() {
        screenshot {
            LearnMoreSheet(
                appBar = speziAppBar {
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
        description = StringResource(MHCStrings.eligibility_subtitle),
        sections = listOf(
            Section(
                title = StringResource(MHCStrings.eligibility_age_title),
                content = BooleanOptionRow(
                    text = StringResource(MHCStrings.eligibility_age_question),
                    subtext = null,
                    value = MutableStateFlow(true),
                    onChange = { },
                    style = BooleanOptionRow.Style.Switch,
                    confirmLabel = StringResource(MHCStrings.answer_yes),
                    declineLabel = StringResource(MHCStrings.answer_no),
                ),
            ),
            Section(
                title = StringResource(MHCStrings.eligibility_region_title),
                content = LinkRow(
                    text = StringResource(MHCStrings.eligibility_region_question),
                    valueLabel = MutableStateFlow(StringResource("Germany")),
                    onClicked = { },
                ),
            ),
            Section(
                title = StringResource(MHCStrings.eligibility_language_title),
                content = BooleanOptionRow(
                    text = StringResource(MHCStrings.eligibility_language_question),
                    subtext = null,
                    value = MutableStateFlow(true),
                    onChange = { },
                    style = BooleanOptionRow.Style.Label,
                    confirmLabel = StringResource(MHCStrings.answer_yes),
                    declineLabel = StringResource(MHCStrings.answer_no),
                ),
            ),
            Section(
                title = StringResource(MHCStrings.eligibility_account_sharing_title),
                content = BooleanOptionRow(
                    text = StringResource(MHCStrings.eligibility_account_sharing_question),
                    subtext = StringResource(MHCStrings.eligibility_account_sharing_explanation),
                    value = MutableStateFlow(false),
                    onChange = { },
                    style = BooleanOptionRow.Style.Label,
                    confirmLabel = StringResource(MHCStrings.answer_yes),
                    declineLabel = StringResource(MHCStrings.answer_no),
                ),
            ),
        ),
        primaryButton = MutableStateFlow(
            AsyncTextButton(
                title = StringResource(MHCStrings.eligibility_save),
                action = { },
            )
        ),
    )

    private fun consentSurveyLayout() = ConsentSurveyLayout(
        header = StringResource(MHCStrings.consent_survey_header),
        questions = listOf(
            BooleanOptionRow(
                text = StringResource(MHCStrings.consent_survey_question_seek_help),
                subtext = null,
                value = MutableStateFlow(true),
                onChange = { },
                style = BooleanOptionRow.Style.Label,
                confirmLabel = StringResource(MHCStrings.answer_true),
                declineLabel = StringResource(MHCStrings.answer_false),
            ),
            BooleanOptionRow(
                text = StringResource(MHCStrings.consent_survey_question_voluntary),
                subtext = null,
                value = MutableStateFlow<Boolean?>(null),
                onChange = { },
                style = BooleanOptionRow.Style.Label,
                confirmLabel = StringResource(MHCStrings.answer_true),
                declineLabel = StringResource(MHCStrings.answer_false),
            ),
            BooleanOptionRow(
                text = StringResource(MHCStrings.consent_survey_question_withdraw),
                subtext = null,
                value = MutableStateFlow(false),
                onChange = { },
                style = BooleanOptionRow.Style.Label,
                confirmLabel = StringResource(MHCStrings.answer_true),
                declineLabel = StringResource(MHCStrings.answer_false),
            ),
        ),
        primaryButton = MutableStateFlow(
            AsyncTextButton(
                title = StringResource(MHCStrings.onboarding_continue),
                action = { },
            )
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

    private fun studyWebsiteLink() = OnboardingLink(
        text = StringResource(MHCStrings.eligibility_learn_more),
        onClicked = { },
    )

    @Composable
    private fun Scaffold(appBarTitle: String? = null, content: @Composable BoxScope.() -> Unit) {
        StaticSpeziScaffold(
            appBar = speziAppBar {
                appBarTitle?.let { title(it) }
                back { }
            },
            content = content,
        )
    }
}
