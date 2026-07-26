//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import edu.stanford.spezi.account.AccountKey
import edu.stanford.spezi.account.AccountKeyOptions
import edu.stanford.spezi.account.InitialValue
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.account.DataDisplayComposable
import edu.stanford.spezi.ui.account.DataEntryComposable
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * Base class for **manual** account keys — values written by app code at specific points and never
 * shown in the sign-up or account-overview UI.
 *
 * Manual keys are [AccountKeyOptions.Mutable] but carry no [AccountKeyOptions.Display] flag, and
 * provide no [DataDisplayComposable]/[DataEntryComposable]. Register them with `manual(key = ...)`.
 *
 * Each concrete key must still be its own `data object` subtype, because account details are keyed
 * by the key's [KClass].
 */
abstract class ManualAccountKey<V : Any>(
    override val identifier: String,
    override val serializer: KSerializer<V>,
    override val initialValue: InitialValue<V>,
    override val valueType: KClass<V>,
) : AccountKey<V> {
    override val options: AccountKeyOptions = AccountKeyOptions.Mutable
    override val name: StringResource = StringResource("")
    override val display: DataDisplayComposable<V>? = null
    override val entry: DataEntryComposable<V>? = null
}
