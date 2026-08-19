//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.LoadingLayout

/**
 * Loading screen shown while the app determines which top-level route to display.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    LoadingLayout(modifier = modifier.fillMaxSize())
}
