//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account

import org.grovealliance.foundation.DefaultProvidingKnowledgeSource

/**
 * An [AccountKey] that is required to be present in the [AccountStorage] for an account to be considered valid.
 */
interface RequiredAccountKey<T : Any> : AccountKey<T>, DefaultProvidingKnowledgeSource<AccountAnchor, T>
