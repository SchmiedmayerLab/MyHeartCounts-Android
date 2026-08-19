//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import org.grovealliance.ui.ComposableBlock
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource

internal sealed interface PrivacyConfig {
    data object Default : PrivacyConfig
    data class Text(
        val title: StringResource,
        val description: StringResource,
    ) : PrivacyConfig
    data class Content(val content: ComposableContent) : PrivacyConfig
    data class Composable(val composable: ComposableBlock) : PrivacyConfig
}
