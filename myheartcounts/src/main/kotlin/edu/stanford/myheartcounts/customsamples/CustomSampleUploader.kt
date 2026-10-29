//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.customsamples

import ca.uhn.fhir.context.FhirContext
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.standard.MHCAppRevision
import edu.stanford.myheartcounts.standard.health.FhirJson
import edu.stanford.myheartcounts.standard.health.HealthObservationProvenance
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.study.StudyManager
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Quantity
import java.util.Date

/**
 * Stores custom observations with stable document identifiers and the participant's provenance.
 */
interface CustomSampleUploader {
    suspend fun upload(samples: List<CustomQuantitySample>): Result<Unit>
}

class CustomSampleUploaderImpl(
    private val firestore: MHCFirestore,
    private val appRevision: MHCAppRevision,
    private val studyManager: StudyManager,
    private val timeProvider: TimeProvider,
) : CustomSampleUploader {
    private val parser by lazy { FhirContext.forR4().newJsonParser() }

    override suspend fun upload(samples: List<CustomQuantitySample>): Result<Unit> = runCatchingAsync {
        val user = firestore.userDocument()
        val enrollment = studyManager.studyEnrollments().firstOrNull()
        val provenance = HealthObservationProvenance(
            appRevision = appRevision,
            studyId = enrollment?.studyId?.toString(),
            studyRevision = enrollment?.studyRevision?.toInt(),
        )
        val encoded = samples.map { sample ->
            require(sample.value.isFinite() && sample.timeRange.endExclusive >= sample.timeRange.start)
            val observation = observation(sample = sample)
            provenance.applyTo(observation = observation)
            sample to FhirJson.toFirestoreMap(json = parser.encodeResourceToString(observation))
        }
        encoded.chunked(BATCH_SIZE).forEach { chunk ->
            user.firestore.batch().apply {
                chunk.forEach { (sample, data) ->
                    val collection = user.collection("${FirebaseSchema.Collection.HEALTH_OBSERVATIONS_PREFIX}${sample.type.id}")
                    set(collection.document(sample.id.toString()), data)
                }
            }.commit().await()
        }
    }

    private fun observation(sample: CustomQuantitySample): Observation = Observation().apply {
        id = sample.id.toString()
        status = Observation.ObservationStatus.FINAL
        addIdentifier().value = sample.id.toString()
        code.addCoding(Coding(SPEZI_SYSTEM, sample.type.id, null))
        if (sample.type == CustomQuantitySampleType.BloodLipids) code.addCoding(Coding(LOINC_SYSTEM, LDL_CODE, null))
        effective = if (sample.timeRange.start == sample.timeRange.endExclusive) {
            DateTimeType(Date.from(sample.timeRange.start))
        } else {
            org.hl7.fhir.r4.model.Period().apply {
                start = Date.from(sample.timeRange.start)
                end = Date.from(sample.timeRange.endExclusive)
            }
        }
        issued = Date.from(timeProvider.nowInstant())
        value = Quantity().apply {
            value = sample.value.toBigDecimal()
            unit = sample.type.unit.symbols.first()
            system = if (sample.type == CustomQuantitySampleType.BloodLipids) LOINC_SYSTEM else SPEZI_SYSTEM
            code = if (sample.type == CustomQuantitySampleType.BloodLipids) LDL_CODE else sample.type.id
        }
    }

    private companion object {
        const val BATCH_SIZE = 100
        const val SPEZI_SYSTEM = "https://spezi.stanford.edu"
        const val LOINC_SYSTEM = "http://loinc.org"
        const val LDL_CODE = "18262-6"
    }
}
