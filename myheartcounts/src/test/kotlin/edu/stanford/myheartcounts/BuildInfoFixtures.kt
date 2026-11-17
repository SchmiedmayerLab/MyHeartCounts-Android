//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import android.os.Build

/**
 * Builds [BuildInfo]s; by default for Android 13.
 */
object BuildInfoFixtures {
    fun create(sdkInt: Int = Build.VERSION_CODES.TIRAMISU) = BuildInfo(sdkInt = sdkInt)
}
