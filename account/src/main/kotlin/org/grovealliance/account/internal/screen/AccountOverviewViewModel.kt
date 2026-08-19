//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKey
import org.grovealliance.account.AccountKeyCategory
import org.grovealliance.account.AccountKeyOptions
import org.grovealliance.account.AccountKeyRequirement
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.AnyAccountKey
import org.grovealliance.account.isAnonymousUser
import org.grovealliance.account.isHiddenCredential
import org.grovealliance.account.userIdType
import org.grovealliance.resources.Strings
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.EventSink
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.LoadingLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.AccountEditButton
import org.grovealliance.ui.account.AccountOverviewItem
import org.grovealliance.ui.account.AccountOverviewLayout
import org.grovealliance.ui.account.AccountOverviewSection
import org.grovealliance.ui.account.AccountProfileHeader
import org.grovealliance.ui.account.StringDataDisplay
import org.grovealliance.ui.coroutinesLauncher
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.showErrorToast
import org.grovealliance.ui.theme.Colors

/**
 * ViewModel for the account overview screen.
 *
 * Builds the overview layout from the currently signed-in user's [AccountDetails], grouping
 * account keys into sections by category. Manages edit mode, triggers bottom sheets for
 * editing individual keys or navigating to name and security overviews, and handles logout.
 * Dismisses the screen automatically when the user signs out.
 */
internal class AccountOverviewViewModel(
    private val account: Account,
    private val accountSheetController: AccountSheetController,
) : ViewModel() {

    private val actionSource = ActionSource(::onAction)
    private val eventSink = EventSink<AccountOverviewEvent>()

    private val _isEditMode = MutableStateFlow(false)
    private val _showEditButton = MutableStateFlow(false)
    private val configuredKeys by lazy { account.configuration.all() }
    private val customSection = MutableStateFlow<ComposableContent?>(null)

    private val editButton = AccountEditButton(
        visible = combine(_isEditMode, _showEditButton) { isEdit, showEdit -> isEdit || showEdit },
        isEditMode = _isEditMode,
        onClick = { _isEditMode.update { !it } },
    )

    private val scaffoldState = mutableScaffoldState(
        appBar = groveAppBar {
            title(Strings.account_overview_title)
            back { eventSink.push(AccountOverviewEvent.Dismissed) }
            action(editButton)
        }
    )

    private val uiState = MutableStateFlow<AccountOverviewState>(AccountOverviewState.Loading)

    val screen = AccountOverviewScreen(
        scaffoldState = scaffoldState,
        state = uiState.asStateFlow(),
        actionSink = actionSource.sink(),
    )

    val events = eventSink.source()

    init {
        viewModelScope.launch {
            merge(
                account.details,
                _isEditMode,
                customSection,
            ).collect {
                val details = account.details.value
                if (details == null) {
                    eventSink.push(AccountOverviewEvent.Dismissed)
                    return@collect
                }
                val hasAbsentOptionalKeys = account.configuration
                    .all(filteredBy = setOf(AccountKeyRequirement.COLLECTED, AccountKeyRequirement.SUPPORTED))
                    .any { key ->
                        key !== AccountKeys.password &&
                            key.display != null &&
                            key.entry != null &&
                            key.options.contains(AccountKeyOptions.Mutable) &&
                            details.getAnyOrNull(key::class) == null
                    }

                _showEditButton.update { hasAbsentOptionalKeys }
                // Auto-exit edit mode when there are no longer any absent optional keys to add.
                if (!_showEditButton.value) _isEditMode.update { false }
                val newLayout = buildLayout(details = details)
                uiState.update { AccountOverviewState.Content(layout = newLayout) }
            }
        }
    }

    private fun onAction(action: AccountOverviewAction) {
        when (action) {
            is AccountOverviewAction.SetCustomSection -> customSection.update { action.content }
        }
    }

    private fun buildLayout(details: AccountDetails) = AccountOverviewLayout(
        title = StringResource(Strings.account_overview_section_title),
        header = buildHeader(details),
        sections = buildSections(details),
        customSection = customSection.value,
        logout = AsyncTextButton(
            title = StringResource(Strings.account_overview_logout_button),
            containerColor = { Colors.error },
            action = ::logout,
        ),
    )

    private fun buildHeader(details: AccountDetails): AccountProfileHeader {
        val name = details[AccountKeys.name::class]?.fullName.orEmpty()
        val userId = details[AccountKeys.userId::class]
        val email = details[AccountKeys.email::class] ?: userId
        val displayName = name.ifBlank { userId }
        val parts = displayName.trim().split("\\s+".toRegex())
        val initials = when {
            parts.isEmpty() -> null
            parts.size == 1 -> parts[0].first().uppercase()
            else -> parts[0].first().uppercase() + parts[1].first().uppercase()
        }
        return AccountProfileHeader(
            initials = initials,
            name = displayName,
            description = email,
        )
    }

    private fun buildSections(details: AccountDetails): List<AccountOverviewSection> {
        val sections = mutableListOf<AccountOverviewSection>()

        // Default section one row for name/identity details, one for sign-in & security.
        val defaultSectionItems = buildList {
            buildNameDetails(details)?.let { add(it) }
            buildSecurityItem(details)?.let { add(it) }
        }
        if (defaultSectionItems.isNotEmpty()) {
            sections.add(AccountOverviewSection(title = StringResource(Strings.account), items = defaultSectionItems))
        }

        // remaining sections (excluding .name and .credentials as covered by default section).
        account.configuration
            .allCategorized(
                filteredBy = setOf(
                    AccountKeyRequirement.REQUIRED,
                    AccountKeyRequirement.COLLECTED,
                    AccountKeyRequirement.SUPPORTED,
                ),
                requiredOptions = AccountKeyOptions.Display,
            )
            .filter { (category, _) ->
                category != AccountKeyCategory.Name && category != AccountKeyCategory.Credentials
            }
            .mapNotNull { (category, keys) ->
                val items = keys.mapNotNull { key -> buildSectionItem(key, details) }
                if (items.isEmpty()) return@mapNotNull null
                AccountOverviewSection(
                    title = category.title,
                    items = items,
                )
            }
            .forEach { sections.add(it) }

        return sections
    }

    private fun buildNameDetails(details: AccountDetails): AccountOverviewItem<String>? {
        val isAnonymousUser = details.isAnonymousUser
        val displaysNameDetails = (account.configuration[AccountKeys.userId::class] != null && !isAnonymousUser) ||
            configuredKeys.any { it.category == AccountKeyCategory.Name }
        if (!displaysNameDetails) return null
        val userIdLabel = details.userIdType.label
        val title = when {
            account.configuration[AccountKeys.name::class] == null -> userIdLabel
            isAnonymousUser -> AccountKeys.name.name
            else -> AccountKeys.name.name + StringResource(", ") + userIdLabel
        }
        return AccountOverviewItem(
            title = title,
            valueDisplay = StringDataDisplay(),
            value = "",
            leadingImage = ImageResource(Icons.Default.AccountCircle),
            showArrow = true,
            onClick = {
                showSheet(
                    AccountSheetRequest.NameOverview(
                        title = title,
                        onEvent = { scaffoldState.dismissBottomSheet() },
                    )
                )
            },
        )
    }

    private fun buildSecurityItem(details: AccountDetails): AccountOverviewItem<String>? {
        val isAnonymousUser = details.isAnonymousUser
        val displaysSecurityItem = !isAnonymousUser &&
            configuredKeys.any { it.category == AccountKeyCategory.Credentials && !it.isHiddenCredential }
        if (!displaysSecurityItem) return null
        return AccountOverviewItem(
            title = StringResource(Strings.account_overview_sign_in_security),
            valueDisplay = StringDataDisplay(),
            value = "",
            leadingImage = ImageResource(Icons.Default.Lock),
            showArrow = true,
            onClick = {
                showSheet(AccountSheetRequest.SecurityOverview(onEvent = { scaffoldState.dismissBottomSheet() }))
            },
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> buildSectionItem(
        key: AccountKey<V>,
        details: AccountDetails,
    ): AccountOverviewItem<V>? {
        val display = key.display
        val currentValue = details.getAnyOrNull(key::class) as? V
        return when {
            display == null -> null
            currentValue != null -> {
                val isEditable = key.entry != null && key.options.contains(AccountKeyOptions.Mutable)
                AccountOverviewItem(
                    title = key.name,
                    valueDisplay = display,
                    value = currentValue,
                    leadingImage = null,
                    showArrow = isEditable,
                    onClick = if (isEditable) {
                        { openEditKey(key, currentValue) }
                    } else {
                        null
                    },
                )
            }

            !_isEditMode.value || key.entry == null || !key.options.contains(AccountKeyOptions.Mutable) -> null
            else -> {
                val initialValue = key.initialValue.value
                AccountOverviewItem(
                    title = key.name,
                    valueDisplay = display,
                    value = initialValue,
                    leadingImage = null,
                    showArrow = true,
                    onClick = { openEditKey(key, initialValue) },
                )
            }
        }
    }

    private fun openEditKey(key: AnyAccountKey, value: Any) {
        showSheet(
            AccountSheetRequest.EditKey(
                key = key,
                currentValue = value,
                onEvent = { scaffoldState.dismissBottomSheet() },
            )
        )
    }

    private fun showSheet(sheetRequest: AccountSheetRequest) {
        val screen = accountSheetController.getSheet(
            request = sheetRequest,
            coroutinesLauncher = coroutinesLauncher,
        )
        scaffoldState.showBottomSheet(screen)
    }

    private suspend fun logout() {
        account.service.logout()
            .onSuccess { eventSink.push(AccountOverviewEvent.Dismissed) }
            .onFailure {
                scaffoldState.showErrorToast(
                    message = StringResource(Strings.account_overview_logout_failed),
                )
            }
    }
}

internal data class AccountOverviewScreen(
    val scaffoldState: GroveScaffoldState,
    val state: StateFlow<AccountOverviewState>,
    val actionSink: ActionSink<AccountOverviewAction>,
)

internal sealed interface AccountOverviewState {
    val layout: ComposableContent

    data object Loading : AccountOverviewState {
        override val layout = LoadingLayout()
    }

    data class Content(override val layout: AccountOverviewLayout) : AccountOverviewState
}

internal sealed interface AccountOverviewEvent {
    data object Dismissed : AccountOverviewEvent
}

internal sealed interface AccountOverviewAction {
    data class SetCustomSection(val content: ComposableContent?) : AccountOverviewAction
}
