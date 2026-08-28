//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Identifier
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

/** Exact identity algorithms defined by the Grove Health Connect 0.3.0 machine contract. */
@Suppress("TooManyFunctions")
internal object HealthConnectIdentity {
    fun recordValue(repositoryScope: String, recordType: String, rawRecordId: String): String {
        require(REPOSITORY_SCOPE.matches(repositoryScope)) {
            "Health Connect repository scope must use canonical lowercase UUID text."
        }
        require(recordType in HealthConnectCatalog.allRecordTypeIdentifiers) {
            "Record type must belong to the closed Health Connect 1.1.0 inventory."
        }
        require(rawRecordId.isNotEmpty()) { "Health Connect raw record id must not be empty." }
        return compose(repositoryScope, recordType, rawRecordId)
    }

    /**
     * The published recording-device identity, or `null` when Health Connect states too little
     * to identify a recorder.
     *
     * One participant's recorder becomes one Device instead of one per record. The subject
     * partitions the key because a wearable belongs to a person, so two participants using the
     * same model stay two devices. Health Connect states no hardware version, so the key rests on
     * the manufacturer and model it does state; a record naming neither has no admitted identity.
     */
    fun recordingDeviceValue(subject: String, manufacturer: String?, model: String?): String? {
        if (subject.isEmpty() || manufacturer.isNullOrEmpty()) {
            return null
        }
        if (model.isNullOrEmpty()) {
            return null
        }
        // Fixed arity of five, so the absent hardware version is an empty component, not a missing one.
        return "v1:" + listOf(subject, "health-connect", manufacturer, model, "")
            .onEach(::requireNoSeparator)
            .joinToString(SEPARATOR)
    }

    /**
     * The sample carries no measured value, so a corrected reading at the same instant keeps its
     * identity and a receiver sees one measurement revised rather than two recorded.
     */
    fun heartRateSampleOutput(
        source: Identifier,
        sampleTime: Instant,
        occurrence: Int,
    ): Identifier {
        require(occurrence >= 0) { "Heart-rate identity requires an unsigned occurrence." }
        return outputIdentifier(
            source,
            listOf(SAMPLE_SELECTOR, sampleTime.utc9(), occurrence.toString()),
        )
    }

    fun seriesSampleOutput(
        source: Identifier,
        sampleTime: Instant,
        occurrence: Int,
    ): Identifier {
        require(occurrence >= 0) { "Series-sample identity requires an unsigned occurrence." }
        return outputIdentifier(
            source,
            listOf(SAMPLE_SELECTOR, sampleTime.utc9(), occurrence.toString()),
        )
    }

    fun nutrientOutput(source: Identifier, nutrientToken: String): Identifier {
        require(nutrientToken in NUTRIENT_TOKENS) {
            "Nutrient identity requires an admitted Health Connect dietary measurement token."
        }
        return outputIdentifier(source, listOf(NUTRIENT_SELECTOR, nutrientToken))
    }

    fun sleepStageOutput(
        source: Identifier,
        start: Instant,
        end: Instant,
        sourceStageToken: String,
        occurrence: Int,
    ): Identifier {
        require(sourceStageToken in SLEEP_STAGE_TOKENS) {
            "Sleep-stage identity requires an exact Health Connect 1.1.0 stage token."
        }
        require(occurrence >= 0) { "Sleep-stage identity requires an unsigned occurrence." }
        return outputIdentifier(
            source,
            listOf(SLEEP_STAGE_SELECTOR, start.utc9(), end.utc9(), sourceStageToken, occurrence.toString()),
        )
    }

    fun segmentOutput(
        source: Identifier,
        start: Instant,
        end: Instant,
        sourceSegmentToken: String,
        occurrence: Int,
    ): Identifier {
        require(sourceSegmentToken in HealthConnectWorkoutVocabulary.segmentIdentityTokens) {
            "Workout-segment identity requires an exact Health Connect 1.1.0 segment token."
        }
        require(occurrence >= 0) { "Workout-segment identity requires an unsigned occurrence." }
        return outputIdentifier(
            source,
            listOf(SEGMENT_SELECTOR, start.utc9(), end.utc9(), sourceSegmentToken, occurrence.toString()),
        )
    }

    fun specimen(source: Identifier, sourceSpecimenToken: String): Identifier {
        require(sourceSpecimenToken in SPECIMEN_TOKENS) {
            "Specimen identity requires an admitted Health Connect specimen token."
        }
        return identifier(
            HealthConnectContract.HEALTH_CONNECT_SPECIMEN_IDENTIFIER,
            source.composedValue() + SEPARATOR + SPECIMEN_SELECTOR + SEPARATOR + requireNoSeparator(sourceSpecimenToken),
        )
    }

    /**
     * The conversion Provenance identity, in the deployment's own namespace.
     *
     * An export event is created here rather than read from Health Connect, so two deployments
     * converting the same Records are expected to differ on it. Everything derived from a Record
     * stays in this guide's namespaces, where they are expected to agree.
     */
    fun conversion(
        graphIdentifierSystem: String,
        repositoryScope: String,
        eventSequence: EventSequence,
    ): Identifier = event(CONVERSION_ROLE, graphIdentifierSystem, repositoryScope, eventSequence)

    /** The exchange Bundle identity, in the deployment's own namespace. See [conversion]. */
    fun exchange(
        graphIdentifierSystem: String,
        repositoryScope: String,
        eventSequence: EventSequence,
    ): Identifier = event(EXCHANGE_ROLE, graphIdentifierSystem, repositoryScope, eventSequence)

    private fun outputIdentifier(source: Identifier, selector: List<String>): Identifier = identifier(
        HealthConnectContract.HEALTH_CONNECT_OUTPUT_IDENTIFIER,
        (listOf(source.composedValue()) + selector.map(::requireNoSeparator)).joinToString(SEPARATOR),
    )

    private fun event(
        role: String,
        graphIdentifierSystem: String,
        repositoryScope: String,
        eventSequence: EventSequence,
    ): Identifier {
        require(REPOSITORY_SCOPE.matches(repositoryScope)) {
            "Health Connect repository scope must use canonical lowercase UUID text."
        }
        require(graphIdentifierSystem.isNotEmpty()) {
            "A deployment graph identifier system is required for an export-created identity."
        }
        // No scheme prefix: the deployment owns this namespace, so this guide does not version it.
        return identifier(
            graphIdentifierSystem,
            listOf(repositoryScope, eventSequence.value, role).joinToString(SEPARATOR),
        )
    }

    /** Recovers the repository scope a Record identifier was composed from. */
    internal fun repositoryScopeOf(source: Identifier): String {
        val value = source.composedValue()
        require(value.startsWith("$SCHEME:")) { "A Health Connect record identifier is a v1 composition." }
        return value.removePrefix("$SCHEME:").substringBefore(SEPARATOR)
    }

    private fun Identifier.composedValue(): String {
        require(hasSystem() && hasValue()) { "A complete source Identifier system and value are required." }
        return value
    }

    private fun Instant.utc9(): String {
        HealthConnectWireFormat.requireFhirInstant(this, "Health Connect identity instant")
        return UTC_NANOSECOND.format(this)
    }

    /** Joins components behind the scheme version. Nothing is hashed, escaped, or re-encoded. */
    private fun compose(vararg components: String): String =
        SCHEME + ":" + components.map(::requireNoSeparator).joinToString(SEPARATOR)

    private fun requireNoSeparator(component: String): String {
        require(!component.contains(SEPARATOR)) {
            "An identity component must not contain a vertical bar; such a value is rejected, never escaped."
        }
        require(component.none { it.isSurrogate() }) {
            "An identity component must not contain an isolated Unicode surrogate."
        }
        return component
    }

    private fun identifier(system: String, value: String): Identifier =
        Identifier().setSystem(system).setValue(value)

    private val UTC_NANOSECOND: DateTimeFormatter =
        DateTimeFormatterBuilder().appendInstant(INSTANT_FRACTION_DIGITS).toFormatter()
    private val REPOSITORY_SCOPE = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private val POSITIVE_DECIMAL = Regex("[1-9][0-9]*")
    private val CANONICAL_DECIMAL = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]*[1-9])?")

    private val NUTRIENT_TOKENS =
        HealthConnectContract.mobileDietaryProfiles.keys + setOf(
            "dietary-energy-from-fat",
            "dietary-fat-trans",
            "dietary-fat-unsaturated",
            "dietary-folic-acid",
        )

    private val SLEEP_STAGE_TOKENS = setOf(
        "STAGE_TYPE_UNKNOWN",
        "STAGE_TYPE_AWAKE",
        "STAGE_TYPE_SLEEPING",
        "STAGE_TYPE_OUT_OF_BED",
        "STAGE_TYPE_LIGHT",
        "STAGE_TYPE_DEEP",
        "STAGE_TYPE_REM",
        "STAGE_TYPE_AWAKE_IN_BED",
    )
    private val SPECIMEN_TOKENS = setOf(
        "SPECIMEN_SOURCE_WHOLE_BLOOD",
        "SPECIMEN_SOURCE_CAPILLARY_BLOOD",
        "SPECIMEN_SOURCE_PLASMA",
        "SPECIMEN_SOURCE_SERUM",
        "SPECIMEN_SOURCE_INTERSTITIAL_FLUID",
    )

    private const val SCHEME = "v1"
    private const val SEPARATOR = "|"
    private const val SPECIMEN_SELECTOR = "specimen"
    private const val SAMPLE_SELECTOR = "sample"
    private const val NUTRIENT_SELECTOR = "nutrient"
    private const val SLEEP_STAGE_SELECTOR = "sleep-stage"
    private const val SEGMENT_SELECTOR = "workout-segment"
    private const val CONVERSION_ROLE = "conversion-provenance"
    private const val EXCHANGE_ROLE = "exchange-bundle"
    private const val INSTANT_FRACTION_DIGITS = 9
}
