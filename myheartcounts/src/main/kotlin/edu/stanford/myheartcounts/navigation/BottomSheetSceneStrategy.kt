//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Presents score details and measurement forms above the existing navigation content.
 */
internal class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (entry.metadata[METADATA_KEY] != true) return null
        return BottomSheetScene(
            entry = entry,
            underlay = entries.dropLast(1),
            onDismiss = onBack,
        )
    }

    companion object {
        private const val METADATA_KEY = "mhc.bottomSheet"

        fun sheet(): Map<String, Any> = mapOf(METADATA_KEY to true)
    }
}

private data class BottomSheetScene<T : Any>(
    val entry: NavEntry<T>,
    val underlay: List<NavEntry<T>>,
    val onDismiss: () -> Unit,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val previousEntries: List<NavEntry<T>> = underlay
    override val overlaidEntries: List<NavEntry<T>> = underlay
    private val sheet = NavigationSheet(
        entry = entry,
        onDismiss = onDismiss,
    )
    override val content: @Composable () -> Unit = { sheet.Sheet(modifier = Modifier) }
}
