//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

/**
 * A [FhirExchangeIdentityStore] returning the secret set through [setSecret], a fixed one until then.
 */
class FakeFhirExchangeIdentityStore : FhirExchangeIdentityStore {
    private var secret = FhirExchangeIdentitySecretFixtures.create()

    /**
     * Sets the secret this store returns.
     */
    fun setSecret(secret: FhirExchangeIdentitySecret) {
        this.secret = secret
    }

    override fun secret(): Result<FhirExchangeIdentitySecret> = Result.success(secret)
}
