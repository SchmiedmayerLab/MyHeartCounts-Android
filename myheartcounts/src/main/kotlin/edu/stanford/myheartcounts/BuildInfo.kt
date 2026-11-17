//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

/**
 * The Android platform the app runs on.
 *
 * @property sdkInt the API level of the running Android version
 */
data class BuildInfo(
    val sdkInt: Int,
)
