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
 * Returns the application [Context] of the [GroveApplication] if [Application] conforms to [GroveApplication] or null otherwise.
 */
val GroveApplication.applicationContext: Context?
    get() = this as? Application

/**
 * Returns the application [Context] of the [GroveApplication].
 *
 * Note that this method will throw in case the [GroveApplication] is not an instance of [Application].
 */
fun GroveApplication.requireApplicationContext(): Context =
    applicationContext ?: groveError("Only android.app.Application is supported as a GroveApplication")
