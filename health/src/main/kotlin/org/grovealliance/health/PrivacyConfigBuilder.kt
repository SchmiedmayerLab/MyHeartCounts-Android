//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health

import org.grovealliance.core.GroveDsl
import org.grovealliance.health.internal.PrivacyConfig
import org.grovealliance.ui.ComposableBlock
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource

@GroveDsl
class PrivacyConfigBuilder {
    internal var config: PrivacyConfig = PrivacyConfig.Default

    fun explanationText(title: StringResource, description: StringResource) {
        config = PrivacyConfig.Text(title = title, description = description)
    }

    fun composable(block: ComposableBlock) {
        config = PrivacyConfig.Composable(block)
    }

    fun content(content: ComposableContent) {
        config = PrivacyConfig.Content(content)
    }
}
