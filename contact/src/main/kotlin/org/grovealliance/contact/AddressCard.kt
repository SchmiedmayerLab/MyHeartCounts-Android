//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.contact

import android.content.Intent
import android.location.Address
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import org.grovealliance.core.logging.GroveLogger
import org.grovealliance.resources.Strings
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Composable
internal fun AddressCard(address: Address, modifier: Modifier = Modifier) {
    GroveCard(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(Spacings.medium)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val addressText = remember(address) { address.formatted() }
            Text(
                text = addressText,
                style = TextStyles.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            val context = LocalContext.current
            IconButton(
                onClick = {
                    runCatching {
                        val addressQuery =
                            URLEncoder.encode(addressText, StandardCharsets.UTF_8.toString())
                        val gmmIntentUri = "geo:0,0?q=$addressQuery".toUri()
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    }.onFailure {
                        GroveLogger.e(it) { "Failed to open intent for address `$addressText`." }
                    }
                },
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = stringResource(Strings.contact_address),
                    tint = Colors.primary,
                )
            }
        }
    }
}

@Composable
@ThemePreviews
private fun AddressCardPreview() {
    GroveTheme {
        AddressCard(Address(Locale.US).apply {
            setAddressLine(0, "1234 Main Street")
            postalCode = "12345"
            locality = "City"
        })
    }
}
