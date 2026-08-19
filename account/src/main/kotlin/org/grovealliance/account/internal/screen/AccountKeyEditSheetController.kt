//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKey
import org.grovealliance.account.AccountKeyType
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.AccountService
import org.grovealliance.account.AnyAccountKey
import org.grovealliance.account.fieldValidationRules
import org.grovealliance.core.coroutines.CoroutinesLauncher
import org.grovealliance.resources.Strings
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.MutableGroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.AccountEditLayout
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.showErrorToast

/**
 * Builds and manages the edit sheet for a single account key.
 *
 * Handles field validation, change detection, and submitting the updated value
 * via [AccountService]. Used by [AccountSheetController] when the user taps an
 * editable row in the name or security overview.
 */
@Suppress("UNCHECKED_CAST")
internal class AccountKeyEditSheetController(
    private val accountService: AccountService,
) {

    private val editValueState by lazy {
        MutableStateFlow(EditValueState(initialValue = Unit, currentValue = Unit, error = null))
    }

    fun buildSheet(
        request: AccountSheetRequest.EditKey,
        coroutinesLauncher: CoroutinesLauncher,
    ): AccountSheet {
        editValueState.update {
            EditValueState(
                initialValue = request.currentValue,
                currentValue = request.currentValue,
                error = null,
            )
        }
        val keyDisplayName = accountService.displayName(request.key)
        val scaffoldState = MutableGroveScaffoldState(
            coroutinesLauncher = coroutinesLauncher,
            appBar = groveAppBar {
                title(keyDisplayName)
                close { request.onEvent(AccountSheetEvent.Dismissed) }
            },
        )

        return AccountSheet(
            scaffoldState = scaffoldState,
            onDismiss = { request.onEvent(AccountSheetEvent.Dismissed) },
            layout = editValueState.map { state ->
                val key = request.key as AccountKey<Any>
                AccountEditLayout(
                    icon = ImageResource(Icons.Default.Edit),
                    title = keyDisplayName,
                    entryComposable = accountService.requireEntryComposable(key),
                    value = state.currentValue,
                    validationMessage = state.error,
                    onValueChange = { newValue ->
                        editValueState.update { it.copy(currentValue = newValue, error = null) }
                    },
                    saveButton = AsyncTextButton(
                        title = StringResource(Strings.account_edit_save_button),
                        enabled = state.hasPendingChange,
                        action = { submitEdit(key, scaffoldState, request.onEvent) },
                    ),
                )
            }
        )
    }

    private suspend fun submitEdit(
        key: AnyAccountKey,
        scaffoldState: MutableGroveScaffoldState,
        onEvent: (AccountSheetEvent) -> Unit,
    ) {
        val value = editValueState.value.currentValue

        if (key.valueType == String::class && value is String) {
            val stringKeyType = key::class as AccountKeyType<String>
            val error = accountService.configuration
                .fieldValidationRules(stringKeyType)
                ?.firstNotNullOfOrNull { it.validate(value)?.message }
            if (error != null) {
                editValueState.update { it.copy(error = error) }
                return
            }
        }

        val modifiedDetails = AccountDetails()
        modifiedDetails.setAny(key::class, value)

        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { modifications ->
                accountService.updateAccountDetails(modifications)
                    .onSuccess { onEvent(AccountSheetEvent.Success) }
                    .getOrThrow()
            }
            .onFailure {
                scaffoldState.showErrorToast(
                    message = StringResource(Strings.account_edit_failed),
                )
            }
    }

    private data class EditValueState(
        val initialValue: Any,
        val currentValue: Any,
        val error: StringResource?,
    ) {
        val hasPendingChange: Boolean get() = currentValue != initialValue
    }
}
