//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class StoredAccountDetails(
    /**
     * The account ID this entry belongs to. This is used as the key for storage and should be unique across accounts.
     */
    val accountId: String,
    /**
     * A map of account key identifiers to their corresponding JSON values.
     * The keys in this map correspond to the identifiers of the [edu.stanford.spezi.account.AccountKey]s,
     * and the values are the serialized JSON representations of the data associated with those keys.
     */
    val accountKeyValues: Map<String, JsonElement>,
)
