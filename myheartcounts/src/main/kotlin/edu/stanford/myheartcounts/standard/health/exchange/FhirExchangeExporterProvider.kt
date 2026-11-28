//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import edu.stanford.myheartcounts.standard.MHCAppRevision
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.fhir.contract.ApplicationDevice
import org.grovealliance.fhir.contract.BusinessIdentifier
import org.grovealliance.fhir.contract.ExchangeProducer
import org.grovealliance.fhir.contract.HostDevice
import org.grovealliance.fhir.contract.IdentifierSystem
import org.grovealliance.fhir.contract.KeyEpoch
import org.grovealliance.fhir.contract.OpaqueIdentityScope
import org.grovealliance.fhir.contract.StudyEnrollment
import org.grovealliance.fhir.contract.Subject
import org.grovealliance.health.fhir.HealthConnectFhirExporter
import org.grovealliance.study.StudyManager

/**
 * The Health Connect exporter for one participant: their pseudonymous subject, this install's
 * identity secret and Health Connect store, the app and phone that convert, and the study they are
 * enrolled in.
 */
interface FhirExchangeExporterProvider {

    /**
     * The exporter for the participant with Firebase account [accountId].
     */
    suspend fun exporter(accountId: String): Result<HealthConnectFhirExporter>
}

/**
 * [FhirExchangeExporterProvider] reading the secret from [identityStore], the converting app and
 * phone from [appRevision], and the enrollment from [studyManager].
 */
class FhirExchangeExporterProviderImpl(
    private val identityStore: FhirExchangeIdentityStore,
    private val appRevision: MHCAppRevision,
    private val studyManager: StudyManager,
) : FhirExchangeExporterProvider {

    override suspend fun exporter(accountId: String): Result<HealthConnectFhirExporter> = runCatchingAsync {
        val secret = identityStore.secret().getOrThrow()
        val subject = identifier(system = PARTICIPANT, value = accountId)
        val epoch = KeyEpoch.of(value = secret.epoch).getOrThrow()
        val scope = OpaqueIdentityScope.create(
            root = system(rawValue = DEPLOYMENT_ROOT),
            keyId = OpaqueIdentityScope.recommendedKeyId(prefix = KEY_ID_PREFIX, key = secret.key, subject = subject).getOrThrow(),
            epoch = epoch,
            key = secret.key,
        ).getOrThrow()
        val producer = ExchangeProducer.create(
            identityScope = scope,
            subject = Subject.Logical(identifier = subject),
            application = ApplicationDevice.create(
                name = APPLICATION_NAME,
                packageName = appRevision.bundleIdentifier,
                version = appRevision.version,
                build = appRevision.build.toString(),
            ).getOrThrow(),
            host = HostDevice.create(
                operatingSystemVersion = appRevision.osVersion,
                operatingSystemName = OPERATING_SYSTEM,
                productType = appRevision.deviceType,
                model = appRevision.deviceModel,
            ).getOrThrow(),
            studies = listOfNotNull(enrollment(accountId = accountId)),
        ).getOrThrow()
        HealthConnectFhirExporter.create(
            producer = producer,
            repositoryScope = identifier(system = REPOSITORY, value = "$REPOSITORY_SOURCE:${secret.storeId}:$accountId"),
            options = HealthConnectFhirExporter.Options(
                userText = HealthConnectFhirExporter.Options.UserText.OMIT,
                writerApplicationSystem = system(rawValue = WRITER_APPLICATION),
                nativeIdentifierSystem = system(rawValue = NATIVE_RECORD),
            ),
        ).getOrThrow()
    }

    /**
     * The participant's enrollment in the study they joined, under its current revision.
     */
    private suspend fun enrollment(accountId: String): StudyEnrollment? {
        val enrollment = studyManager.studyEnrollments().firstOrNull() ?: return null
        val studyId = enrollment.studyId.toString().lowercase()
        return StudyEnrollment.create(
            study = identifier(system = RESEARCH_STUDY, value = studyId),
            protocolUrl = "$DEPLOYMENT_ROOT/PlanDefinition/$studyId",
            protocolVersion = enrollment.studyRevision.toString(),
            enrollment = identifier(system = RESEARCH_SUBJECT, value = "$studyId:$accountId"),
        ).getOrThrow()
    }

    private fun identifier(system: String, value: String) = BusinessIdentifier.create(
        system = system(rawValue = system),
        value = value,
    ).getOrThrow()

    private fun system(rawValue: String) = IdentifierSystem.create(rawValue = rawValue).getOrThrow()

    private companion object {
        const val DEPLOYMENT_ROOT = "https://myheartcounts.stanford.edu/fhir"
        const val PARTICIPANT = "$DEPLOYMENT_ROOT/identifiers/participant"
        const val RESEARCH_STUDY = "$DEPLOYMENT_ROOT/identifiers/research-study"
        const val RESEARCH_SUBJECT = "$DEPLOYMENT_ROOT/identifiers/research-subject"
        const val REPOSITORY = "$DEPLOYMENT_ROOT/identifiers/repository"
        const val NATIVE_RECORD = "$DEPLOYMENT_ROOT/identifiers/health-connect-record"
        const val WRITER_APPLICATION = "$DEPLOYMENT_ROOT/identifiers/android-application"
        const val REPOSITORY_SOURCE = "health-connect"
        const val KEY_ID_PREFIX = "s"
        const val APPLICATION_NAME = "My Heart Counts"
        const val OPERATING_SYSTEM = "Android"
    }
}
