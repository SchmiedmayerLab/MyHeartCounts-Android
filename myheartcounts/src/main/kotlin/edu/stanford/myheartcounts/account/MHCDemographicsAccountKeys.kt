//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

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
import edu.stanford.spezi.account.AccountDetails
import edu.stanford.spezi.account.AccountKeys
import edu.stanford.spezi.account.InitialValue
import kotlinx.serialization.builtins.serializer

/**
 * Demographics account keys. All are **manual** keys (see [ManualAccountKey]).
 */

// String / numeric

/**
 * First 3 digits of the user's US ZIP code.
 */
data object UsZipCodePrefixKey : ManualAccountKey<String>(
    identifier = "usZipCodePrefix",
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * First half of the user's UK postcode.
 */
data object UkPostcodePrefixKey : ManualAccountKey<String>(
    identifier = "ukPostcodePrefix",
    serializer = String.serializer(),
    initialValue = InitialValue.string,
    valueType = String::class,
)

/**
 * The user's height in centimeters.
 */
data object HeightInCmKey : ManualAccountKey<Double>(
    identifier = "heightInCM",
    serializer = Double.serializer(),
    initialValue = InitialValue.empty(0.0),
    valueType = Double::class,
)

/**
 * The user's weight in kilograms.
 */
data object WeightInKgKey : ManualAccountKey<Double>(
    identifier = "weightInKG",
    serializer = Double.serializer(),
    initialValue = InitialValue.empty(0.0),
    valueType = Double::class,
)

/**
 * Whether the user consents to being contacted about future studies.
 */
data object FutureStudiesKey : ManualAccountKey<Boolean>(
    identifier = "futureStudies",
    serializer = Boolean.serializer(),
    initialValue = InitialValue.empty(false),
    valueType = Boolean::class,
)

// Enum-backed

/**
 * The user's gender identity.
 */
data object MHCGenderIdentityKey : ManualAccountKey<MHCGenderIdentity>(
    identifier = "mhcGenderIdentity",
    serializer = MHCGenderIdentity.serializer(),
    initialValue = InitialValue.empty(MHCGenderIdentity.PREFER_NOT_TO_STATE),
    valueType = MHCGenderIdentity::class,
)

/**
 * The user's US state / region.
 */
data object UsRegionKey : ManualAccountKey<USRegion>(
    identifier = "usRegion",
    serializer = USRegion.serializer(),
    initialValue = InitialValue.empty(USRegion.NOT_SET),
    valueType = USRegion::class,
)

/**
 * The user's US household income bracket.
 */
data object HouseholdIncomeUSKey : ManualAccountKey<HouseholdIncomeUS>(
    identifier = "householdIncomeUS",
    serializer = HouseholdIncomeUS.serializer(),
    initialValue = InitialValue.empty(HouseholdIncomeUS.NOT_SET),
    valueType = HouseholdIncomeUS::class,
)

/**
 * The user's UK household income bracket.
 */
data object HouseholdIncomeUKKey : ManualAccountKey<HouseholdIncomeUK>(
    identifier = "householdIncomeUK",
    serializer = HouseholdIncomeUK.serializer(),
    initialValue = InitialValue.empty(HouseholdIncomeUK.NOT_SET),
    valueType = HouseholdIncomeUK::class,
)

/**
 * The user's US educational level.
 */
data object EducationUSKey : ManualAccountKey<EducationStatusUS>(
    identifier = "educationUS",
    serializer = EducationStatusUS.serializer(),
    initialValue = InitialValue.empty(EducationStatusUS.NOT_SET),
    valueType = EducationStatusUS::class,
)

/**
 * The user's UK educational level.
 */
data object EducationUKKey : ManualAccountKey<EducationStatusUK>(
    identifier = "educationUK",
    serializer = EducationStatusUK.serializer(),
    initialValue = InitialValue.empty(EducationStatusUK.NOT_SET),
    valueType = EducationStatusUK::class,
)

/**
 * Whether the user identifies as Latino / Hispanic.
 */
data object LatinoStatusKey : ManualAccountKey<LatinoStatusOption>(
    identifier = "latinoStatus",
    serializer = LatinoStatusOption.serializer(),
    initialValue = InitialValue.empty(LatinoStatusOption.NOT_SET),
    valueType = LatinoStatusOption::class,
)

/**
 * The user's biological sex at birth.
 */
data object BiologicalSexAtBirthKey : ManualAccountKey<BiologicalSex>(
    identifier = "biologicalSexAtBirth",
    serializer = BiologicalSex.serializer(),
    initialValue = InitialValue.empty(BiologicalSex.PREFER_NOT_TO_STATE),
    valueType = BiologicalSex::class,
)

/**
 * The user's blood type.
 */
data object BloodTypeKey : ManualAccountKey<BloodType>(
    identifier = "bloodType",
    serializer = BloodType.serializer(),
    initialValue = InitialValue.empty(BloodType.NOT_SET),
    valueType = BloodType::class,
)

/**
 * The user's behaviour-change stage.
 */
data object StageOfChangeKey : ManualAccountKey<StageOfChangeOption>(
    identifier = "stageOfChange",
    serializer = StageOfChangeOption.serializer(),
    initialValue = InitialValue.empty(StageOfChangeOption.NOT_SET),
    valueType = StageOfChangeOption::class,
)

/**
 * The user's UK region.
 */
data object UkRegionKey : ManualAccountKey<UKRegion>(
    identifier = "ukRegion",
    serializer = UKRegion.serializer(),
    initialValue = InitialValue.empty(UKRegion.NotSet),
    valueType = UKRegion::class,
)

/**
 * The user's race / ethnicity (a set of options).
 */
data object RaceEthnicityKey : ManualAccountKey<RaceEthnicity>(
    identifier = "raceEthnicity",
    serializer = RaceEthnicity.serializer(),
    initialValue = InitialValue.empty(RaceEthnicity.NONE),
    valueType = RaceEthnicity::class,
)

/**
 * The user's selected comorbidities.
 */
data object ComorbiditiesKey : ManualAccountKey<Comorbidities>(
    identifier = "comorbidities",
    serializer = Comorbidities.serializer(),
    initialValue = InitialValue.empty(Comorbidities(emptyMap())),
    valueType = Comorbidities::class,
)

/**
 * The user's NHS number (UK only).
 */
data object NhsNumberKey : ManualAccountKey<NHSNumber>(
    identifier = "nhsNumber",
    serializer = NHSNumber.serializer(),
    initialValue = InitialValue.empty(NHSNumber("")),
    valueType = NHSNumber::class,
)

// AccountKeys access

val AccountKeys.usZipCodePrefix get() = UsZipCodePrefixKey
val AccountKeys.ukPostcodePrefix get() = UkPostcodePrefixKey
val AccountKeys.heightInCm get() = HeightInCmKey
val AccountKeys.weightInKg get() = WeightInKgKey
val AccountKeys.futureStudies get() = FutureStudiesKey
val AccountKeys.mhcGenderIdentity get() = MHCGenderIdentityKey
val AccountKeys.usRegion get() = UsRegionKey
val AccountKeys.householdIncomeUS get() = HouseholdIncomeUSKey
val AccountKeys.householdIncomeUK get() = HouseholdIncomeUKKey
val AccountKeys.educationUS get() = EducationUSKey
val AccountKeys.educationUK get() = EducationUKKey
val AccountKeys.latinoStatus get() = LatinoStatusKey
val AccountKeys.biologicalSexAtBirth get() = BiologicalSexAtBirthKey
val AccountKeys.bloodType get() = BloodTypeKey
val AccountKeys.stageOfChange get() = StageOfChangeKey
val AccountKeys.ukRegion get() = UkRegionKey
val AccountKeys.raceEthnicity get() = RaceEthnicityKey
val AccountKeys.comorbidities get() = ComorbiditiesKey
val AccountKeys.nhsNumber get() = NhsNumberKey

// AccountDetails access

val AccountDetails.usZipCodePrefix: String? get() = this[UsZipCodePrefixKey::class]
val AccountDetails.ukPostcodePrefix: String? get() = this[UkPostcodePrefixKey::class]
val AccountDetails.heightInCm: Double? get() = this[HeightInCmKey::class]
val AccountDetails.weightInKg: Double? get() = this[WeightInKgKey::class]
val AccountDetails.futureStudies: Boolean? get() = this[FutureStudiesKey::class]
val AccountDetails.mhcGenderIdentity: MHCGenderIdentity? get() = this[MHCGenderIdentityKey::class]
val AccountDetails.usRegion: USRegion? get() = this[UsRegionKey::class]
val AccountDetails.householdIncomeUS: HouseholdIncomeUS? get() = this[HouseholdIncomeUSKey::class]
val AccountDetails.householdIncomeUK: HouseholdIncomeUK? get() = this[HouseholdIncomeUKKey::class]
val AccountDetails.educationUS: EducationStatusUS? get() = this[EducationUSKey::class]
val AccountDetails.educationUK: EducationStatusUK? get() = this[EducationUKKey::class]
val AccountDetails.latinoStatus: LatinoStatusOption? get() = this[LatinoStatusKey::class]
val AccountDetails.biologicalSexAtBirth: BiologicalSex? get() = this[BiologicalSexAtBirthKey::class]
val AccountDetails.bloodType: BloodType? get() = this[BloodTypeKey::class]
val AccountDetails.stageOfChange: StageOfChangeOption? get() = this[StageOfChangeKey::class]
val AccountDetails.ukRegion: UKRegion? get() = this[UkRegionKey::class]
val AccountDetails.raceEthnicity: RaceEthnicity? get() = this[RaceEthnicityKey::class]
val AccountDetails.comorbidities: Comorbidities? get() = this[ComorbiditiesKey::class]
val AccountDetails.nhsNumber: NHSNumber? get() = this[NhsNumberKey::class]
