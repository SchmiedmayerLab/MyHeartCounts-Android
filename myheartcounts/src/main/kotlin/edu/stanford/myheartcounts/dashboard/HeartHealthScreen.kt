package edu.stanford.myheartcounts.dashboard

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * The heart health dashboard tab, visualizing the participant's health metrics.
 */
@Composable
fun HeartHealthScreen() {
    val viewModel = speziViewModel<HeartHealthViewModel>()
    viewModel.screen.Content()
}
