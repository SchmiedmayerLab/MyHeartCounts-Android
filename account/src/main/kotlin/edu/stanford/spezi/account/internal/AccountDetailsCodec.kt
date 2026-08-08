//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account.internal

import edu.stanford.spezi.account.AccountDetails
import edu.stanford.spezi.account.AccountDetailsCodecConfig
import edu.stanford.spezi.account.AccountKey
import edu.stanford.spezi.account.AccountKeys
import edu.stanford.spezi.account.AnyAccountKey
import edu.stanford.spezi.account.keys
import edu.stanford.spezi.core.Module
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.foundation.JsonSerializer

@Suppress("UNCHECKED_CAST")
internal class AccountDetailsCodec : Module {
    private val codecConfig by dependency<AccountDetailsCodecConfig>()

    fun encode(
        accountId: String,
        details: AccountDetails,
    ): StoredAccountDetails {
        val keyValues = buildMap {
            details.accountKeyTypes.keys().forEach { key ->
                if (key.identifier == AccountKeys.accountId.identifier) return@forEach
                val typedKey = key as AccountKey<Any>
                val value = details.getAnyOrNull(typedKey::class) ?: return@forEach

                put(
                    key = codecConfig.encodingIdentifier(typedKey),
                    value = JsonSerializer.encodeToElement(value, typedKey.serializer),
                )
            }
        }

        return StoredAccountDetails(
            accountId = accountId,
            accountKeyValues = keyValues,
        )
    }

    fun decode(stored: StoredAccountDetails, keys: Set<AnyAccountKey>): AccountDetails {
        val details = AccountDetails()

        stored.accountKeyValues.forEach { (keyId, jsonElement) ->
            val key = codecConfig.resolveDecodingKey(storedIdentifier = keyId, requestedKeys = keys) ?: return@forEach
            val typedKey = key as AccountKey<Any>

            val value = JsonSerializer.decodeFromElementOrNull(jsonElement, typedKey.serializer)
                ?: return@forEach

            details.setAny(typedKey::class, value)
        }

        details[AccountKeys.accountId::class] = stored.accountId
        return details
    }
}
