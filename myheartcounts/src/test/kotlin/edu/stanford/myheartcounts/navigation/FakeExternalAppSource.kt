//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.navigation

/**
 * An [ExternalAppSource] reporting the apps set through [setCanOpen] as openable, every app by
 * default.
 */
class FakeExternalAppSource : ExternalAppSource {
    private var canOpen: (NavigationEvent.ExternalApp) -> Boolean = { true }

    /**
     * Makes [canOpen] answer every following request with [canOpen].
     */
    fun setCanOpen(canOpen: (NavigationEvent.ExternalApp) -> Boolean) {
        this.canOpen = canOpen
    }

    override fun canOpen(app: NavigationEvent.ExternalApp): Boolean = canOpen.invoke(app)
}
