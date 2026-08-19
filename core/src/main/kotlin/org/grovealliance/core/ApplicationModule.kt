//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core

import android.app.Application
import android.content.Context

/**
 * An automatically registered module during app start up that provides access to the grove application instance.
 */
class ApplicationModule internal constructor(val application: GroveApplication) : Module {
    val configuration: Configuration
        get() = application.configuration
    val standard: Standard
        get() = configuration.standard

    override fun equals(other: Any?): Boolean {
        return other is ApplicationModule && other.application == application
    }

    /**
     * Returns the application [Context] of the [GroveApplication].
     * Note that this method will throw in case the [GroveApplication] is not an instance of [Application].
     */
    fun requireContext(): Context = application.requireApplicationContext()

    /**
     * Configures the module by delegating to the [standard] configuration.
     */
    override fun configure() {
        standard.configure()
    }

    override fun hashCode(): Int {
        return application.hashCode()
    }

    override fun toString(): String {
        return "ApplicationModule[${application.applicationContext?.packageName ?: "null"}]"
    }
}
