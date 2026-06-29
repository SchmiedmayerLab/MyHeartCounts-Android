package edu.stanford.myheartcounts.home

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * The main content shown after onboarding completes.
 */
@Composable
fun HomeScreen() {
    val viewModel = speziViewModel<HomeViewModel>()
    viewModel.screen.Content()
}
