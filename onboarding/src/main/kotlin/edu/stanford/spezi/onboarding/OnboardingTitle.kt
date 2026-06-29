package edu.stanford.spezi.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * Displays the title area for an onboarding page.
 *
 * @property icon Optional image centered above the text.
 * @property title Primary heading for the page.
 * @property subtitle Optional supporting text shown below [title].
 * @property customSubtitle Optional custom content shown below the text subtitle.
 */
data class OnboardingTitle(
    val icon: ImageResource? = null,
    val title: StringResource,
    val subtitle: StringResource? = null,
    val customSubtitle: ComposableContent? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacings.medium),
                    contentAlignment = Alignment.Center
                ) {
                    it.Content(modifier = Modifier.size(Sizes.Icon.extraLarge))
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title.text(),
                style = TextStyles.headlineSmall.bold(),
            )
            subtitle?.let { sub ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = sub.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurfaceVariant,
                )
            }
            customSubtitle?.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    SpeziTheme {
        OnboardingTitle(
            icon = ImageResource(image = Icons.Outlined.Map),
            title = StringResource("Welcome to the Study"),
            subtitle = StringResource("Join thousands of participants helping advance heart health research."),
        ).Content()
    }
}
