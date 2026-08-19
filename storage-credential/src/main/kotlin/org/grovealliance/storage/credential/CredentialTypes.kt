//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.storage.credential

import java.util.EnumSet

data class CredentialTypes(
    internal val set: EnumSet<CredentialType>,
) {
    companion object {
        val All = CredentialTypes(EnumSet.allOf(CredentialType::class.java))
        val Server = CredentialTypes(EnumSet.of(CredentialType.SERVER))
        val NonServer = CredentialTypes(EnumSet.of(CredentialType.NON_SERVER))
    }
}

enum class CredentialType {
    SERVER, NON_SERVER
}
