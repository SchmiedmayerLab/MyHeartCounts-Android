//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.contact

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.foundation.UUID
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import java.util.UUID

/**
 * ContactOption data class used to represent a contact option.
 *
 * @param id the unique identifier of the contact option
 * @param image the image of the contact option
 * @param title the title of the contact option
 * @param action the action of the contact option
 */
data class ContactOption(
    val id: UUID = UUID(),
    val image: ImageVector?,
    val title: StringResource,
    val action: (Context) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        GroveCard(
            modifier = modifier
                .defaultMinSize(80.dp)
                .fillMaxWidth()
                .clickable {
                    action(context)
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(Spacings.medium)
                    .fillMaxWidth()
            ) {
                Icon(
                    image ?: Icons.Default.Email,
                    contentDescription = title.text(),
                    tint = Colors.primary,
                )
                Text(
                    text = title.text(),
                    maxLines = 1,
                )
            }
        }
    }

    companion object {
        internal val logger by groveLogger()
    }
}

@Composable
@ThemePreviews
private fun ContactOptionCardPreview() {
    GroveTheme {
        val option = ContactOption.text(
            number = "+1 (650) 723-2300"
        )
        option.Content()
    }
}
