package edu.stanford.myheartcounts.upcoming

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * The upcoming tasks tab, listing the participant's scheduled activities.
 */
@Composable
fun UpcomingTasksScreen() {
    val viewModel = speziViewModel<UpcomingTasksViewModel>()
    viewModel.screen.Content()
}
