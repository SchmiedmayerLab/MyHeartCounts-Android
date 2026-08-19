//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import org.grovealliance.core.GroveApplication

/**
 * A [ContentProvider] that initializes the [GroveApplication] when the application is created.
 *
 * This is used to ensure that the GroveApplication is configured automatically before any other components
 * in the application.
 */
internal class GroveApplicationContentProvider : ContentProvider() {
    private val logger by groveCoreLogger()

    override fun onCreate(): Boolean {
        logger.i { "Initializing GroveApplicationContentProvider" }
        val application = context?.applicationContext as? GroveApplication
        if (application != null) {
            logger.i { "Grove application available. Configuring Grove" }
            GroveApplication.configure(application = application)
        } else {
            logger.w { "Grove application not available. Skipping configuration for context: ${context?.packageName ?: "null"}" }
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
