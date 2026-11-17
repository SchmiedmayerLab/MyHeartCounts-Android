//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.promptedactions.PromptedActionRejections
import edu.stanford.myheartcounts.promptedactions.PromptedActionRejectionsImpl
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMapper
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMapperImpl
import edu.stanford.myheartcounts.promptedactions.PromptedActionsSource
import edu.stanford.myheartcounts.promptedactions.PromptedActionsSourceImpl
import org.grovealliance.core.ConfigurationBuilder

/**
 * Registers the actions the participant is prompted to take: their states, the rejected ones and
 * their presentation.
 */
fun ConfigurationBuilder.promptedActionsConfigurations() {
    singleton<PromptedActionsSource> {
        PromptedActionsSourceImpl(
            account = dependency(),
            demographicsEditor = dependency(),
            studyManager = dependency(),
            studyActivation = dependency(),
            rejections = dependency(),
        )
    }
    singleton<PromptedActionRejections> {
        PromptedActionRejectionsImpl(
            storage = dependency(),
        )
    }
    singleton<PromptedActionsMapper> { PromptedActionsMapperImpl() }
}
