//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.standard.health.HealthStagingDatabase
import edu.stanford.myheartcounts.standard.health.exchange.ExchangeChunkPacker
import edu.stanford.myheartcounts.standard.health.exchange.ExchangeChunkPackerImpl
import edu.stanford.myheartcounts.standard.health.exchange.FhirExchangeExporterProvider
import edu.stanford.myheartcounts.standard.health.exchange.FhirExchangeExporterProviderImpl
import edu.stanford.myheartcounts.standard.health.exchange.FhirExchangeIdentityStore
import edu.stanford.myheartcounts.standard.health.exchange.FhirExchangeIdentityStoreImpl
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangeDrain
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangeDrainImpl
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangePipeline
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangeStaging
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangeStagingImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType

/**
 * Registers the record-centric health upload path: the exchange identity, the exporter, staging and the drain.
 */
fun ConfigurationBuilder.healthExchangeConfigurations() {
    singleton<FhirExchangeIdentityStore> {
        FhirExchangeIdentityStoreImpl(
            storage = dependency<KeyValueStorageFactory>().create(fileName = "mhc-fhir-exchange", type = KeyValueStorageType.ENCRYPTED),
        )
    }
    factory<FhirExchangeExporterProvider> {
        FhirExchangeExporterProviderImpl(
            identityStore = dependency(),
            appRevision = dependency(),
            studyManager = dependency(),
        )
    }
    factory<ExchangeChunkPacker> { ExchangeChunkPackerImpl() }
    factory<HealthExchangeStaging> {
        HealthExchangeStagingImpl(
            records = dependency<HealthStagingDatabase>().exchangeRecords(),
            exporterProvider = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<HealthExchangeDrain> {
        HealthExchangeDrainImpl(
            records = dependency<HealthStagingDatabase>().exchangeRecords(),
            packer = dependency(),
            managedUpload = dependency(),
            timeProvider = dependency(),
        )
    }
    factory {
        HealthExchangePipeline(
            staging = dependency(),
            drain = dependency(),
        )
    }
}
