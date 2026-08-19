//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.grovealliance.resources.Strings
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * Layout for the "Change Password" bottom sheet.
 *
 * Renders two password fields ("New Password" and "Confirm Password") with a single inline
 * error message anchored below the second field.
 *
 * @param newPassword Current value of the "New Password" field.
 * @param confirmPassword Current value of the "Confirm Password" field.
 * @param onNewPasswordChange Callback invoked when the new-password field changes.
 * @param onConfirmPasswordChange Callback invoked when the confirm-password field changes.
 * @param validationMessage Inline error shown below the second field (`null` = no error).
 * @param saveButton The primary action button (enabled only when the form is valid + dirty).
 */
data class AccountChangePasswordLayout(
    val newPassword: String,
    val confirmPassword: String,
    val onNewPasswordChange: (String) -> Unit,
    val onConfirmPasswordChange: (String) -> Unit,
    val validationMessage: StringResource?,
    val saveButton: AsyncTextButton,
    val icon: ImageResource,
) : ComposableContent {

    private val newPasswordEntry = StringDataEntry(
        placeholder = StringResource(Strings.account_change_password_new_password),
        hideContent = true,
    )
    private val confirmPasswordEntry = StringDataEntry(
        placeholder = StringResource(Strings.account_change_password_confirm_password),
        hideContent = true,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .padding(Spacings.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.medium))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Text(
                    modifier = Modifier.padding(top = Spacings.small),
                    text = stringResource(Strings.account_change_password_new_password),
                    style = TextStyles.bodyMedium.bold(),
                )
                newPasswordEntry.Content(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    modifier = Modifier.padding(top = Spacings.small),
                    text = stringResource(Strings.account_change_password_confirm_password),
                    style = TextStyles.bodyMedium.bold(),
                )
                confirmPasswordEntry.Content(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                validationMessage?.let {
                    Text(
                        text = it.text(),
                        color = Colors.error,
                        style = TextStyles.bodySmall,
                    )
                }
            }

            saveButton.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

@ThemePreviews
@Composable
private fun PreviewNoError() {
    GroveTheme {
        AccountChangePasswordLayout(
            newPassword = "hunter2",
            confirmPassword = "hunter2",
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            validationMessage = null,
            saveButton = AsyncTextButton(title = StringResource("Save"), action = {}),
            icon = ImageResource(Icons.Default.Lock),
        ).Content(modifier = Modifier.fillMaxSize())
    }
}

@ThemePreviews
@Composable
private fun PreviewWithError() {
    GroveTheme {
        AccountChangePasswordLayout(
            newPassword = "abc",
            confirmPassword = "xyz",
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            validationMessage = StringResource("Passwords do not match."),
            saveButton = AsyncTextButton(title = StringResource("Save"), action = {}),
            icon = ImageResource(Icons.Default.Lock),
        ).Content(modifier = Modifier.fillMaxSize())
    }
}
