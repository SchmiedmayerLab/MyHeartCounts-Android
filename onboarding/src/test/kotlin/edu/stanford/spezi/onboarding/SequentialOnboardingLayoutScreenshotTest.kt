package edu.stanford.spezi.onboarding

import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.StringResource
import org.junit.Test

@Suppress("MaxLineLength")
class SequentialOnboardingLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `SequentialOnboardingLayout first step screenshot`() {
        screenshot {
            SequentialOnboardingLayout(
                title = StringResource("How It Works"),
                subtitle = StringResource("Follow these steps to join the study."),
                steps = steps,
                actionText = StringResource("Get Started"),
                action = {},
            ).Content()
        }
    }

    @Test
    fun `SequentialOnboardingLayout last step screenshot`() {
        screenshot {
            SequentialOnboardingLayout(
                title = StringResource("How It Works"),
                subtitle = StringResource("Follow these steps to join the study."),
                steps = steps,
                initialStepIndex = steps.size - 1,
                actionText = StringResource("Get Started"),
                action = {},
            ).Content()
        }
    }

    private val steps = listOf(
        SequentialOnboardingLayout.Step(
            title = StringResource("Create your account"),
            description = StringResource("Sign up with your email address to get started. Your account keeps your data safe and synced across devices."),
        ),
        SequentialOnboardingLayout.Step(
            title = StringResource("Review and sign consent"),
            description = StringResource("Read through the informed consent form carefully and provide your digital signature to confirm your participation."),
        ),
        SequentialOnboardingLayout.Step(
            title = StringResource("Grant health permissions"),
            description = StringResource("Allow access to Health Connect so we can collect the heart health data needed for the study."),
        ),
        SequentialOnboardingLayout.Step(
            title = StringResource("Start contributing"),
            description = StringResource("That's it! Your device will begin collecting data in the background. You'll receive updates as the study progresses."),
        ),
    )
}
