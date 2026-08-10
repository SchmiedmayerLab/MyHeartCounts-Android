//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.core

import android.app.Application
import android.content.Context

/**
 * Returns the application [Context] of the [SpeziApplication] if [Application] conforms to [SpeziApplication] or null otherwise.
 */
val SpeziApplication.applicationContext: Context?
    get() = this as? Application

/**
 * Returns the application [Context] of the [SpeziApplication].
 *
 * Note that this method will throw in case the [SpeziApplication] is not an instance of [Application].
 */
fun SpeziApplication.requireApplicationContext(): Context =
    applicationContext ?: speziError("Only android.app.Application is supported as a SpeziApplication")
