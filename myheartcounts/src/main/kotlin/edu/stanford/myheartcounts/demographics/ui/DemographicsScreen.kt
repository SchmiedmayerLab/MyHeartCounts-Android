//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.demographics.DemographicsViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The demographics questions, presented as [mode] describes.
 */
@Composable
fun DemographicsScreen(mode: DemographicsScreenMode) {
    val viewModel = groveViewModel<DemographicsViewModel> {
        putString(DemographicsViewModel.KEY_MODE, mode.name)
    }
    viewModel.screen.Content()
}

/**
 * How the [DemographicsScreen] is presented.
 */
enum class DemographicsScreenMode {
    /**
     * A screen of its own, beneath an app bar that closes it.
     */
    Complete,

    /**
     * Without an app bar, inside a host that provides its own.
     */
    Embedded,
}
