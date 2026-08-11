//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.testing.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import edu.stanford.spezi.ui.SemanticKeys
import edu.stanford.spezi.ui.TestIdentifier
import edu.stanford.spezi.ui.tag

fun SemanticsNodeInteractionsProvider.onNodeWithIdentifier(
    identifier: TestIdentifier,
    suffix: String? = null,
    useUnmergedTree: Boolean = false,
) = onNodeWithTag(identifier.tag(suffix = suffix), useUnmergedTree = useUnmergedTree)

fun SemanticsNodeInteractionsProvider.onNodeWithContent(content: String) = onNode(
    matcher = SemanticsMatcher.expectValue(SemanticKeys.Content, content),
    useUnmergedTree = true,
)

fun SemanticsNodeInteractionsProvider.onAllNodes(
    identifier: TestIdentifier,
    useUnmergedTree: Boolean = false,
) = onAllNodesWithTag(identifier.tag(), useUnmergedTree)

fun SemanticsNodeInteractionsProvider.waitNode(
    identifier: TestIdentifier,
) = onAllNodesWithTag(identifier.tag()).fetchSemanticsNodes().isNotEmpty()
