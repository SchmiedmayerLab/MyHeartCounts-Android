package edu.stanford.spezi.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

@Suppress("MaxLineLength")
class OnboardingLayoutScreenshotTest : ScreenshotTest() {

    private val areas = listOf(
        OnboardingArea(
            icon = ImageResource(image = Icons.Outlined.Favorite),
            title = StringResource("Heart Health Tracking"),
            description = StringResource("Monitor your heart rate, activity, and other vital signs to help researchers understand cardiovascular patterns."),
        ),
        OnboardingArea(
            icon = ImageResource(image = Icons.Outlined.AutoAwesome),
            title = StringResource("Research Impact"),
            description = StringResource("Your data contributes to groundbreaking studies that could change how heart disease is prevented and treated."),
        ),
        OnboardingArea(
            icon = ImageResource(image = Icons.Outlined.Security),
            title = StringResource("Privacy Protected"),
            description = StringResource("All data is encrypted and anonymised. You control what you share and can withdraw at any time."),
        ),
        OnboardingArea(
            icon = ImageResource(image = Icons.Outlined.Notifications),
            title = StringResource("Stay Informed"),
            description = StringResource("Receive updates on study findings and how your contributions are making a difference."),
        ),
    )

    @Test
    fun `OnboardingLayout with title subtitle and four areas screenshot`() {
        screenshot {
            OnboardingLayout(
                title = StringResource("Welcome to the Study"),
                subtitle = StringResource("Join thousands of participants helping advance cardiovascular research."),
                areas = areas,
                actionText = StringResource("Get Started"),
                action = {},
            ).Content()
        }
    }

    @Test
    fun `OnboardingLayout without subtitle screenshot`() {
        screenshot {
            OnboardingLayout(
                title = StringResource("Heart Health Research"),
                areas = areas,
                actionText = StringResource("Continue"),
                action = {},
            ).Content()
        }
    }

    @Test
    fun `OnboardingLayout with primary and secondary buttons screenshot`() {
        screenshot {
            OnboardingLayout(
                header = OnboardingTitle(
                    title = StringResource("Privacy & Data"),
                    subtitle = StringResource("Understand how your data is used and protected."),
                ),
                content = OnboardingInformation(areas = areas.take(3)),
                footer = OnboardingActions(
                    primaryButton = AsyncTextButton(
                        title = StringResource("I Agree"),
                        action = {},
                    ),
                    secondaryButton = AsyncTextButton(
                        title = StringResource("Learn More"),
                        containerColor = { Colors.transparent },
                        textColor = { Colors.primary },
                        action = {},
                    ),
                ),
            ).Content()
        }
    }

    @Test
    fun `OnboardingLayout with icon and description only areas screenshot`() {
        screenshot {
            OnboardingLayout(
                header = OnboardingTitle(
                    icon = ImageResource(image = Icons.Outlined.HealthAndSafety),
                    title = StringResource("Health Data Access"),
                    subtitle = StringResource("Choose which health information you want to share with the study."),
                ),
                content = OnboardingInformation(
                    areas = listOf(
                        OnboardingArea(
                            icon = ImageResource(image = Icons.Outlined.Lightbulb),
                            title = null,
                            description = StringResource("Review why each permission helps researchers understand participant activity."),
                        ),
                        OnboardingArea(
                            icon = ImageResource(image = Icons.Outlined.Watch),
                            title = null,
                            description = StringResource("You can update access later in your device settings."),
                        ),
                    ),
                ),
                footer = OnboardingActions(
                    primaryButton = AsyncTextButton(
                        title = StringResource("Continue"),
                        action = {},
                    ),
                ),
            ).Content()
        }
    }

    @Test
    fun `OnboardingLayout with custom subtitle screenshot`() {
        screenshot {
            OnboardingLayout(
                header = OnboardingTitle(
                    title = StringResource("Ineligible for Participation"),
                    subtitle = StringResource("You are not eligible to participate in this study at this time."),
                    customSubtitle = IneligibleSubtitleContent(
                        linkText = StringResource("Visit the study website"),
                    ),
                ),
                content = OnboardingInformation(areas = emptyList()),
                wrapInScrollView = false,
                footer = null,
            ).Content()
        }
    }

    private data class IneligibleSubtitleContent(
        val linkText: StringResource,
    ) : ComposableContent {

        @Composable
        override fun Content(modifier: Modifier) {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                AsyncTextButton(
                    title = linkText,
                    containerColor = { Colors.transparent },
                    textColor = { Colors.primary },
                    action = {},
                ).Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
