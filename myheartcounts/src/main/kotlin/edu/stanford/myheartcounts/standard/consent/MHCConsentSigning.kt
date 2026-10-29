//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import edu.stanford.myheartcounts.account.lastSignedConsentVersion
import org.grovealliance.account.Account
import org.grovealliance.foundation.SemanticVersion

/**
 * Decides whether the signed-in participant has to sign the study's consent document.
 */
interface MHCConsentSigning {

    /**
     * Whether the participant has to sign the current consent document: they never signed one, or
     * the current version supersedes theirs by a minor or major version. A patch version alone does
     * not require signing again.
     *
     * The participant's account details are read as they are now, so callers wait for them to load
     * first. When either version cannot be determined, signing is required.
     */
    suspend fun isSigningRequired(): Boolean
}

/**
 * [MHCConsentSigning] comparing the account's last signed version with the bundled consent document.
 */
class MHCConsentSigningImpl(
    private val account: Account,
    private val documentProvider: MHCConsentDocumentProvider,
) : MHCConsentSigning {

    override suspend fun isSigningRequired(): Boolean {
        val currentVersion = runCatching { documentProvider.document().metadata.version }.getOrNull() ?: return true
        val signedVersion = account.details.value?.lastSignedConsentVersion?.let { SemanticVersion(it) } ?: return true
        return currentVersion.major > signedVersion.major ||
            (currentVersion.major == signedVersion.major && currentVersion.minor > signedVersion.minor)
    }
}
