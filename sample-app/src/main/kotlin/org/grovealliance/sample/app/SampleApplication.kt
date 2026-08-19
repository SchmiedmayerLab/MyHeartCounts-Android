//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app

import android.app.Application
import androidx.health.connect.client.records.Record
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.InMemoryAccountService
import org.grovealliance.account.InMemoryAccountStorageProvider
import org.grovealliance.account.accountConfiguration
import org.grovealliance.core.Configuration
import org.grovealliance.core.GroveApplication
import org.grovealliance.core.logging.GroveLogger
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.viewmodel.viewModel
import org.grovealliance.health.CollectionMode
import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.RecordType
import org.grovealliance.health.health
import org.grovealliance.sample.app.health.HealthPrivacyScreen
import org.grovealliance.sample.app.health.HealthViewModel
import org.grovealliance.sample.app.home.HomeViewModel
import kotlin.time.Duration.Companion.seconds

class SampleApplication : Application(), GroveApplication, HealthConstraint {
    private val logger by groveLogger()

    override val configuration: Configuration = Configuration(standard = this) {
        singleton { Navigator() }

        viewModel {
            HomeViewModel(
                navigator = dependency(),
                account = dependency(),
            )
        }
        viewModel {
            HealthViewModel(
                navigator = dependency(),
                health = dependency(),
            )
        }

        health {
            requestReadAccess(RecordType.bloodPressure, RecordType.weight)
            requestWriteAccess(RecordType.heartRate, RecordType.steps)
            collectRecord(
                recordType = RecordType.steps,
                start = CollectionMode.Automatic(pollingInterval = 5.seconds),
                continueInBackground = true,
            )
            privacy {
                composable { HealthPrivacyScreen() }
            }
        }

        accountConfiguration(
            service = InMemoryAccountService(),
            storageProvider = InMemoryAccountStorageProvider(),
            configuration = {
                requires(key = AccountKeys.accountId)
                collects(key = AccountKeys.name)
                collects(key = AccountKeys.email)
                collects(key = AccountKeys.password)
                collects(key = AccountKeys.dateOfBirth)
                supports(key = AccountKeys.genderIdentity)
                manual(key = AccountKeys.userId)
            }
        )
    }

    override fun onCreate() {
        super.onCreate()

        GroveLogger.setLoggingEnabled(enabled = true)
    }

    override suspend fun <T : Record> handleDeletedRecords(deletedRecordIds: Set<String>, type: RecordType<out T>) {
        logger.i { "Received deleted records callback: $deletedRecordIds" }
    }

    override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) {
        logger.i { "Received added records callback of type: ${type.type}" }
    }

    override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
        logger.i { "Received on fully resync required callback of type: ${type.type}" }
    }
}
