package edu.stanford.myheartcounts.study

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * The study content shown after onboarding, hosting the bottom-navigation tabs.
 */
@Composable
fun StudyScreen() {
    val viewModel = speziViewModel<StudyViewModel>()
    viewModel.screen.Content()
}
