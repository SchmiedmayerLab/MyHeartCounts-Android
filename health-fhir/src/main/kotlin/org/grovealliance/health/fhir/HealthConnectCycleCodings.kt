//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord

/** Maps each Health Connect cycle-tracking constant onto its shared Grove coding. */
internal fun menstruationFlowCoding(flow: Int): SourceCodedValue = when (flow) {
    MenstruationFlowRecord.FLOW_UNKNOWN ->
        SourceCodedValue("unspecified", "Unspecified", "FLOW_UNKNOWN", "Unknown")
    MenstruationFlowRecord.FLOW_LIGHT ->
        SourceCodedValue("light", "Light", "FLOW_LIGHT", "Light")
    MenstruationFlowRecord.FLOW_MEDIUM ->
        SourceCodedValue("medium", "Medium", "FLOW_MEDIUM", "Medium")
    MenstruationFlowRecord.FLOW_HEAVY ->
        SourceCodedValue("heavy", "Heavy", "FLOW_HEAVY", "Heavy")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect menstruation flow: $flow")
}

internal fun ovulationTestCoding(result: Int): SourceCodedValue = when (result) {
    OvulationTestRecord.RESULT_NEGATIVE ->
        SourceCodedValue("negative", "Negative", "RESULT_NEGATIVE", "Negative")
    OvulationTestRecord.RESULT_HIGH ->
        SourceCodedValue("high-fertility", "High fertility", "RESULT_HIGH", "High")
    OvulationTestRecord.RESULT_POSITIVE ->
        SourceCodedValue(
            "luteinizing-hormone-surge",
            "Luteinizing hormone surge",
            "RESULT_POSITIVE",
            "Positive",
        )
    OvulationTestRecord.RESULT_INCONCLUSIVE ->
        SourceCodedValue("indeterminate", "Indeterminate", "RESULT_INCONCLUSIVE", "Inconclusive")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect ovulation-test result: $result")
}

internal fun sexualActivityCoding(protectionUsed: Int): SourceCodedValue = when (protectionUsed) {
    SexualActivityRecord.PROTECTION_USED_UNKNOWN ->
        SourceCodedValue("unknown", "Unknown", "PROTECTION_USED_UNKNOWN", "Unknown")
    SexualActivityRecord.PROTECTION_USED_PROTECTED ->
        SourceCodedValue("protected", "Protection used", "PROTECTION_USED_PROTECTED", "Protected")
    SexualActivityRecord.PROTECTION_USED_UNPROTECTED ->
        SourceCodedValue(
            "unprotected",
            "Protection not used",
            "PROTECTION_USED_UNPROTECTED",
            "Unprotected",
        )
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect protection use: $protectionUsed")
}

internal fun cervicalMucusAppearanceCoding(appearance: Int): SourceCodedValue = when (appearance) {
    CervicalMucusRecord.APPEARANCE_UNKNOWN ->
        SourceCodedValue("unknown", "Unknown", "APPEARANCE_UNKNOWN", "Unknown")
    CervicalMucusRecord.APPEARANCE_DRY ->
        SourceCodedValue("dry", "Dry", "APPEARANCE_DRY", "Dry")
    CervicalMucusRecord.APPEARANCE_STICKY ->
        SourceCodedValue("sticky", "Sticky", "APPEARANCE_STICKY", "Sticky")
    CervicalMucusRecord.APPEARANCE_CREAMY ->
        SourceCodedValue("creamy", "Creamy", "APPEARANCE_CREAMY", "Creamy")
    CervicalMucusRecord.APPEARANCE_WATERY ->
        SourceCodedValue("watery", "Watery", "APPEARANCE_WATERY", "Watery")
    CervicalMucusRecord.APPEARANCE_EGG_WHITE ->
        SourceCodedValue("egg-white", "Egg white", "APPEARANCE_EGG_WHITE", "Egg white")
    CervicalMucusRecord.APPEARANCE_UNUSUAL ->
        SourceCodedValue("unusual", "Unusual", "APPEARANCE_UNUSUAL", "Unusual")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect cervical-mucus appearance: $appearance")
}

internal fun cervicalMucusSensationCoding(sensation: Int): SourceCodedValue? = when (sensation) {
    CervicalMucusRecord.SENSATION_UNKNOWN -> null
    CervicalMucusRecord.SENSATION_LIGHT ->
        SourceCodedValue("light", "Light", "SENSATION_LIGHT", "Light")
    CervicalMucusRecord.SENSATION_MEDIUM ->
        SourceCodedValue("medium", "Medium", "SENSATION_MEDIUM", "Medium")
    CervicalMucusRecord.SENSATION_HEAVY ->
        SourceCodedValue("heavy", "Heavy", "SENSATION_HEAVY", "Heavy")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect cervical-mucus sensation: $sensation")
}
