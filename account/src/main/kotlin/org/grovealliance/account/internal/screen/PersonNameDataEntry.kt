//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.grovealliance.account.PersonName
import org.grovealliance.resources.Strings
import org.grovealliance.ui.DescriptionGridRow
import org.grovealliance.ui.GroveInputFieldComposable
import org.grovealliance.ui.account.DataEntryComposable
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

internal class PersonNameDataEntry : DataEntryComposable<PersonName> {
    @Composable
    override fun Content(value: PersonName, onValueChange: (PersonName) -> Unit, modifier: Modifier) {
        Column(modifier = modifier) {
            DescriptionGridRow(
                description = { Text(text = stringResource(Strings.account_name_given_name)) }
            ) {
                GroveInputFieldComposable(
                    value = value.givenName,
                    placeholder = stringResource(Strings.account_name_given_name_placeholder)
                ) {
                    onValueChange(value.copy(givenName = it))
                }
            }

            DescriptionGridRow(
                description = { Text(text = stringResource(Strings.account_name_family_name)) }
            ) {
                GroveInputFieldComposable(
                    value = value.familyName,
                    placeholder = stringResource(Strings.account_name_family_name_placeholder)
                ) {
                    onValueChange(value.copy(familyName = it))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val entry = PersonNameDataEntry()

    GroveTheme {
        entry.Content(value = PersonName(fullName = "John Doe"), onValueChange = {})
    }
}
