package edu.stanford.myheartcounts.onboarding

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * Entry point for the onboarding flow, rendering the content owned by the [OnboardingViewModel].
 */
@Composable
fun OnboardingScreen() {
    val viewModel = speziViewModel<OnboardingViewModel>()
    viewModel.content.Content()
}
