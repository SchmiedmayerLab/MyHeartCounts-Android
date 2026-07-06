package edu.stanford.spezi.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.VerticalSpacer
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

/**
 * Arranges an onboarding page from an optional title, body content, and optional footer actions.
 *
 * @property header Optional title area shown above the onboarding content.
 * @property content Main information shown in the page body.
 * @property footer Optional actions anchored below the page body.
 * @property wrapInScrollView Whether the page body scrolls independently of the footer.
 */
class OnboardingLayout(
    val header: OnboardingTitle? = null,
    val content: OnboardingInformation,
    val footer: OnboardingActions?,
    val wrapInScrollView: Boolean = true,
) : ComposableContent {

    /**
     * Creates a page with a text title, an optional subtitle, a list of information areas, and one primary action.
     *
     * @param title Title shown at the top of the page.
     * @param subtitle Optional supporting text shown below [title].
     * @param areas Information rows shown in the page body.
     * @param actionText Label for the primary action.
     * @param action Work performed when the primary action is selected.
     */
    constructor(
        title: StringResource,
        subtitle: StringResource? = null,
        areas: List<OnboardingArea>,
        actionText: StringResource,
        action: suspend () -> Unit,
    ) : this(
        header = OnboardingTitle(title = title, subtitle = subtitle),
        content = OnboardingInformation(areas = areas),
        footer = OnboardingActions(
            primaryButton = AsyncTextButton(title = actionText, action = action),
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacings.medium),
        ) {
            val scrollModifier = if (wrapInScrollView) {
                Modifier.weight(1f).verticalScroll(rememberScrollState())
            } else {
                Modifier.weight(1f)
            }
            Column(modifier = scrollModifier) {
                header?.Content()
                VerticalSpacer(height = Spacings.large)
                content.Content()
            }
            footer?.Content()
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    SpeziTheme {
        OnboardingLayout(
            header = OnboardingTitle(
                title = StringResource("Ineligible for Participation"),
                subtitle = StringResource("You are not eligible to participate in this study."),
            ),
            content = OnboardingInformation(emptyList()),
            wrapInScrollView = false,
            footer = null,
        ).Content()
    }
}
