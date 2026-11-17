//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.navigation

import android.content.Context

/**
 * Which requests to leave the app an installed app can handle.
 */
interface ExternalAppSource {

    /**
     * Whether an installed app can open [app]. An app is only seen when the app's manifest
     * declares the kind of intent opening it among its queries.
     */
    fun canOpen(app: NavigationEvent.ExternalApp): Boolean
}

/**
 * Default [ExternalAppSource] implementation, asking the package manager for an activity handling
 * the intent the [intentMapper] opens [app] with.
 */
class ExternalAppSourceImpl(
    private val context: Context,
    private val intentMapper: ExternalAppIntentMapper,
) : ExternalAppSource {

    override fun canOpen(app: NavigationEvent.ExternalApp): Boolean {
        val intent = intentMapper.mapIntent(app = app)
        return context.packageManager.queryIntentActivities(intent.selector ?: intent, 0).isNotEmpty()
    }
}
