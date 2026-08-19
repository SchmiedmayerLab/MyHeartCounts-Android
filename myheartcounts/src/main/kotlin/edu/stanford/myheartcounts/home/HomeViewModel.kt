//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.lifecycle.ViewModel
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.ui.PlaceholderScreenContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the Home tab, exposing its placeholder screen content and app bar.
 */
class HomeViewModel(
    studyAppBarProvider: StudyAppBarProvider,
) : ViewModel() {
    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create { title(MHCStrings.app_name) },
    )

    val screen = PlaceholderScreenContent(
        scaffoldState = scaffoldState,
        title = StringResource(MHCStrings.home_tab_home),
    )
}
