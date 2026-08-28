//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import java.time.Instant

internal fun HealthConnectConverter.convertMenstruationFlow(
    record: MenstruationFlowRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantCoded(
    metadata = record.metadata,
    recordType = HealthConnectConverter.MENSTRUATION_FLOW_RECORD,
    time = record.time,
    offset = record.zoneOffset,
    spec = MobileCodedSpec(
        profile = HealthConnectContract.MOBILE_MENSTRUATION_FLOW_PROFILE,
        category = null,
        code = "menstruation-flow",
        display = "Menstruation flow",
    ),
    value = codedValue(
        HealthConnectContract.GROVE_MENSTRUATION_FLOW,
        HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_FLOW,
        menstruationFlowCoding(record.flow),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertOvulationTest(
    record: OvulationTestRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantCoded(
    metadata = record.metadata,
    recordType = HealthConnectConverter.OVULATION_TEST_RECORD,
    time = record.time,
    offset = record.zoneOffset,
    spec = MobileCodedSpec(
        profile = HealthConnectContract.MOBILE_OVULATION_TEST_RESULT_PROFILE,
        category = null,
        code = "ovulation-test-result",
        display = "Ovulation test result",
    ),
    value = codedValue(
        HealthConnectContract.GROVE_OVULATION_TEST_RESULT,
        HealthConnectContract.HEALTH_CONNECT_OVULATION_TEST_RESULT,
        ovulationTestCoding(record.result),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertSexualActivity(
    record: SexualActivityRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantCoded(
    metadata = record.metadata,
    recordType = HealthConnectConverter.SEXUAL_ACTIVITY_RECORD,
    time = record.time,
    offset = record.zoneOffset,
    spec = MobileCodedSpec(
        profile = HealthConnectContract.MOBILE_SEXUAL_ACTIVITY_PROFILE,
        category = null,
        code = "sexual-activity",
        display = "Sexual activity",
    ),
    value = codedValue(
        HealthConnectContract.GROVE_SEXUAL_ACTIVITY,
        HealthConnectContract.HEALTH_CONNECT_SEXUAL_ACTIVITY_PROTECTION,
        sexualActivityCoding(record.protectionUsed),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertCervicalMucus(
    record: CervicalMucusRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val component = cervicalMucusSensationCoding(record.sensation)?.let { sensation ->
        Observation.ObservationComponentComponent().apply {
            code = concept(
                HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                "cervical-mucus-sensation",
                "Cervical mucus sensation",
            )
            value = codedValue(
                HealthConnectContract.GROVE_CERVICAL_MUCUS_SENSATION,
                HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_SENSATION,
                sensation,
            )
        }
    }
    return convertInstantCoded(
        metadata = record.metadata,
        recordType = HealthConnectConverter.CERVICAL_MUCUS_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.MOBILE_CERVICAL_MUCUS_QUALITY_PROFILE,
            category = null,
            code = "cervical-mucus-quality",
            display = "Cervical mucus quality",
        ),
        value = codedValue(
            HealthConnectContract.GROVE_CERVICAL_MUCUS_QUALITY,
            HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_APPEARANCE,
            cervicalMucusAppearanceCoding(record.appearance),
        ),
        component = component,
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )
}

internal fun HealthConnectConverter.convertIntermenstrualBleeding(
    record: IntermenstrualBleedingRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantCoded(
    metadata = record.metadata,
    recordType = HealthConnectConverter.INTERMENSTRUAL_BLEEDING_RECORD,
    time = record.time,
    offset = record.zoneOffset,
    spec = MobileCodedSpec(
        profile = HealthConnectContract.MOBILE_INTERMENSTRUAL_BLEEDING_PROFILE,
        category = null,
        code = "intermenstrual-bleeding",
        display = "Intermenstrual bleeding",
    ),
    value = CodeableConcept(
        Coding(HealthConnectContract.GROVE_INTERMENSTRUAL_BLEEDING, "present", "Present"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertMenstruationPeriod(
    record: MenstruationPeriodRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertPeriodCoded(
    metadata = record.metadata,
    recordType = HealthConnectConverter.MENSTRUATION_PERIOD_RECORD,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    spec = MobileCodedSpec(
        profile = HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD_PROFILE,
        category = null,
        code = "menstruation-period",
        display = "Menstruation period",
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        adapterSpecific = true,
    ),
    value = CodeableConcept(
        Coding(HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD, "present", "Present"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)
