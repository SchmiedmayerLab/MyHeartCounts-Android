package edu.stanford.spezi.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
 * Shows a single onboarding information row with an icon and optional text.
 *
 * @property icon Visual marker for the row.
 * @property title Optional headline for the row.
 * @property description Optional supporting text for the row.
 */
data class OnboardingArea(
    val icon: ImageResource,
    val title: StringResource?,
    val description: StringResource?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.large))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                title?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.titleMedium.bold(),
                    )
                }

                description?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    SpeziTheme {
        OnboardingArea(
            icon = ImageResource(image = Icons.Default.Favorite),
            title = StringResource("Heart Health"),
            description = StringResource("Track your heart rate and activity to help researchers understand cardiovascular patterns."),
        ).Content()
    }
}
