//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core

/**
 * A custom exception class used in the Grove framework.
 *
 * @param message The error message to be displayed.
 * @param cause The underlying cause of the error, if any.
 */
class GroveError(message: String, cause: Throwable?) : Throwable(message, cause)

/**
 * A custom error function that throws a [GroveError] with the provided message and optional cause.
 *
 * @param message The error message .
 * @param cause The underlying cause of the error, if any.
 * @return Nothing
 */
fun groveError(message: String, cause: Throwable? = null): Nothing = throw GroveError(message, cause)
