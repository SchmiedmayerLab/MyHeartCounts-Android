package edu.stanford.myheartcounts.dashboard

import androidx.lifecycle.ViewModel
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.ui.PlaceholderScreenContent
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.mutableScaffoldState

/**
 * Backs the Heart Health tab, exposing its placeholder screen content and app bar.
 */
class HeartHealthViewModel(
    studyAppBarProvider: StudyAppBarProvider,
) : ViewModel() {
    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create { title(MHCStrings.home_tab_heart_health) },
    )

    val screen = PlaceholderScreenContent(
        scaffoldState = scaffoldState,
        title = StringResource(MHCStrings.home_tab_heart_health),
    )
}
