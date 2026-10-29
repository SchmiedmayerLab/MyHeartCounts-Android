//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.demographics.ui.DemographicsDatePicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsEntrySheet
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.DemographicsMenu
import edu.stanford.myheartcounts.demographics.ui.DemographicsMonthYearPicker
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.EducationStatusUS
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUS
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import java.time.LocalDate

/**
 * An interaction with the [DemographicsLayout].
 */
sealed interface DemographicsAction {

    /**
     * The participant asked to fill in answers from health data.
     */
    data object HealthImportSelected : DemographicsAction

    /**
     * The participant tapped the question for [field].
     */
    data class QuestionSelected(val field: DemographicsField) : DemographicsAction

    /**
     * The participant dismissed the open [DemographicsMenu] without choosing an answer.
     */
    data object MenuDismissed : DemographicsAction

    /**
     * The participant closed the open [DemographicsDatePicker] without choosing a date.
     */
    data object DatePickerDismissed : DemographicsAction

    /**
     * The participant edited the entry of the open [DemographicsEntrySheet].
     */
    data class EntryChanged(val entry: String) : DemographicsAction

    /**
     * The participant saved the entry of the open [DemographicsEntrySheet].
     */
    data object EntrySaved : DemographicsAction

    /**
     * The participant asked where to find their NHS number.
     */
    data object NHSNumberHelpSelected : DemographicsAction

    /**
     * The participant chose the UK [nation] whose counties to choose from.
     */
    data class UKNationSelected(val nation: String) : DemographicsAction

    /**
     * The participant closed the open sheet.
     */
    data object SheetClosed : DemographicsAction

    /**
     * The participant tapped the date of diagnosis of the comorbidity [condition].
     */
    data class DiagnosisDateSelected(val condition: String) : DemographicsAction

    /**
     * The participant closed the open [DemographicsMonthYearPicker] without changing the date.
     */
    data object MonthYearPickerDismissed : DemographicsAction

    /**
     * The participant changed one answer of the [DemographicsResponse]; `null` clears it.
     */
    sealed interface ResponseChanged : DemographicsAction

    data class DateOfBirthChanged(val value: LocalDate?) : ResponseChanged

    data class GenderIdentityChanged(val value: MHCGenderIdentity?) : ResponseChanged

    data class BiologicalSexAtBirthChanged(val value: BiologicalSex?) : ResponseChanged

    data class BloodTypeChanged(val value: BloodType?) : ResponseChanged

    data class HeightChanged(val value: Double?) : ResponseChanged

    data class WeightChanged(val value: Double?) : ResponseChanged

    data class RaceEthnicityChanged(val value: RaceEthnicity?) : ResponseChanged

    data class LatinoStatusChanged(val value: LatinoStatusOption?) : ResponseChanged

    data class ComorbiditiesChanged(val value: Comorbidities?) : ResponseChanged

    data class USRegionChanged(val value: USRegion?) : ResponseChanged

    data class UKRegionChanged(val value: UKRegion?) : ResponseChanged

    data class EducationUSChanged(val value: EducationStatusUS?) : ResponseChanged

    data class EducationUKChanged(val value: EducationStatusUK?) : ResponseChanged

    data class HouseholdIncomeUSChanged(val value: HouseholdIncomeUS?) : ResponseChanged

    data class HouseholdIncomeUKChanged(val value: HouseholdIncomeUK?) : ResponseChanged

    data class NHSNumberChanged(val value: NHSNumber?) : ResponseChanged

    data class StageOfChangeChanged(val value: StageOfChangeOption?) : ResponseChanged

    data class ReferralSourceChanged(val value: String?) : ResponseChanged
}
