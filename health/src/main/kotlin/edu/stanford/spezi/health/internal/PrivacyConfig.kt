//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.health.internal

import edu.stanford.spezi.ui.ComposableBlock
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource

internal sealed interface PrivacyConfig {
    data object Default : PrivacyConfig
    data class Text(
        val title: StringResource,
        val description: StringResource,
    ) : PrivacyConfig
    data class Content(val content: ComposableContent) : PrivacyConfig
    data class Composable(val composable: ComposableBlock) : PrivacyConfig
}
