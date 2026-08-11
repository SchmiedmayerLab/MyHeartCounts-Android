//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.contact

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.core.net.toUri
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.StringResource

fun ContactOption.Companion.website(uriString: String): ContactOption =
    ContactOption(
        image = Icons.Default.Info,
        title = StringResource(Strings.contact_website),
        action = { context ->
            runCatching {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, uriString.toUri())
                context.startActivity(browserIntent)
            }.onFailure {
                logger.e(it) { "Failed to open intent for website at `$uriString`." }
            }
        }
    )
