//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.account.BiologicalSexAtBirthKey
import edu.stanford.myheartcounts.account.BloodTypeKey
import edu.stanford.myheartcounts.account.ComorbiditiesKey
import edu.stanford.myheartcounts.account.EducationUKKey
import edu.stanford.myheartcounts.account.EducationUSKey
import edu.stanford.myheartcounts.account.HeightInCmKey
import edu.stanford.myheartcounts.account.HouseholdIncomeUKKey
import edu.stanford.myheartcounts.account.HouseholdIncomeUSKey
import edu.stanford.myheartcounts.account.LatinoStatusKey
import edu.stanford.myheartcounts.account.MHCGenderIdentityKey
import edu.stanford.myheartcounts.account.NhsNumberKey
import edu.stanford.myheartcounts.account.RaceEthnicityKey
import edu.stanford.myheartcounts.account.ReferralSourceKey
import edu.stanford.myheartcounts.account.StageOfChangeKey
import edu.stanford.myheartcounts.account.UkRegionKey
import edu.stanford.myheartcounts.account.UsRegionKey
import edu.stanford.myheartcounts.account.WeightInKgKey
import org.grovealliance.account.AccountAnchor
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.DateOfBirthKey
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.foundation.KnowledgeSourceType
import java.time.LocalTime

/**
 * Converts between the participant's account details and their [DemographicsResponse].
 */
interface DemographicsCodec {

    /**
     * The demographic answers stored in [details].
     */
    fun read(details: AccountDetails): DemographicsResponse

    /**
     * The questions answered differently in [first] and [second].
     */
    fun differences(first: DemographicsResponse, second: DemographicsResponse): Set<DemographicsField>

    /**
     * [target] with the answers to [fields] taken from [source].
     */
    fun merged(
        target: DemographicsResponse,
        source: DemographicsResponse,
        fields: Set<DemographicsField>,
    ): DemographicsResponse

    /**
     * The account modifications turning the stored [baseline] into [response]: changed answers are
     * written and cleared ones removed.
     */
    fun changes(baseline: DemographicsResponse, response: DemographicsResponse): Result<AccountModifications>
}

/**
 * Default [DemographicsCodec] implementation. A date of birth is stored as noon of that day in the
 * device's time zone.
 */
class DemographicsCodecImpl(
    private val timeProvider: TimeProvider,
) : DemographicsCodec {

    private val bindings: List<Binding<*>> = listOf(
        Binding(
            field = DemographicsField.DateOfBirth,
            key = DateOfBirthKey::class,
            get = { response ->
                response.dateOfBirth?.atTime(LocalTime.NOON)?.atZone(timeProvider.currentZone())?.toInstant()
            },
            set = { response, value ->
                response.copy(dateOfBirth = value?.atZone(timeProvider.currentZone())?.toLocalDate())
            },
        ),
        Binding(
            field = DemographicsField.GenderIdentity,
            key = MHCGenderIdentityKey::class,
            get = { it.genderIdentity },
            set = { response, value -> response.copy(genderIdentity = value) },
        ),
        Binding(
            field = DemographicsField.SexAtBirth,
            key = BiologicalSexAtBirthKey::class,
            get = { it.biologicalSexAtBirth },
            set = { response, value -> response.copy(biologicalSexAtBirth = value) },
        ),
        Binding(
            field = DemographicsField.BloodType,
            key = BloodTypeKey::class,
            get = { it.bloodType },
            set = { response, value -> response.copy(bloodType = value) },
        ),
        Binding(
            field = DemographicsField.Height,
            key = HeightInCmKey::class,
            get = { it.heightInCm },
            set = { response, value -> response.copy(heightInCm = value) },
        ),
        Binding(
            field = DemographicsField.Weight,
            key = WeightInKgKey::class,
            get = { it.weightInKg },
            set = { response, value -> response.copy(weightInKg = value) },
        ),
        Binding(
            field = DemographicsField.RaceEthnicity,
            key = RaceEthnicityKey::class,
            get = { it.raceEthnicity },
            set = { response, value -> response.copy(raceEthnicity = value) },
        ),
        Binding(
            field = DemographicsField.LatinoStatus,
            key = LatinoStatusKey::class,
            get = { it.latinoStatus },
            set = { response, value -> response.copy(latinoStatus = value) },
        ),
        Binding(
            field = DemographicsField.Comorbidities,
            key = ComorbiditiesKey::class,
            get = { it.comorbidities },
            set = { response, value -> response.copy(comorbidities = value) },
        ),
        Binding(
            field = DemographicsField.USRegion,
            key = UsRegionKey::class,
            get = { it.usRegion },
            set = { response, value -> response.copy(usRegion = value) },
        ),
        Binding(
            field = DemographicsField.UKRegion,
            key = UkRegionKey::class,
            get = { it.ukRegion },
            set = { response, value -> response.copy(ukRegion = value) },
        ),
        Binding(
            field = DemographicsField.USEducation,
            key = EducationUSKey::class,
            get = { it.educationUS },
            set = { response, value -> response.copy(educationUS = value) },
        ),
        Binding(
            field = DemographicsField.UKEducation,
            key = EducationUKKey::class,
            get = { it.educationUK },
            set = { response, value -> response.copy(educationUK = value) },
        ),
        Binding(
            field = DemographicsField.USIncome,
            key = HouseholdIncomeUSKey::class,
            get = { it.householdIncomeUS },
            set = { response, value -> response.copy(householdIncomeUS = value) },
        ),
        Binding(
            field = DemographicsField.UKIncome,
            key = HouseholdIncomeUKKey::class,
            get = { it.householdIncomeUK },
            set = { response, value -> response.copy(householdIncomeUK = value) },
        ),
        Binding(
            field = DemographicsField.NHSNumber,
            key = NhsNumberKey::class,
            get = { it.nhsNumber },
            set = { response, value -> response.copy(nhsNumber = value) },
        ),
        Binding(
            field = DemographicsField.StageOfChange,
            key = StageOfChangeKey::class,
            get = { it.stageOfChange },
            set = { response, value -> response.copy(stageOfChange = value) },
        ),
        Binding(
            field = DemographicsField.ReferralSource,
            key = ReferralSourceKey::class,
            get = { it.referralSource },
            set = { response, value -> response.copy(referralSource = value) },
        ),
    )

    override fun read(details: AccountDetails): DemographicsResponse =
        bindings.fold(DemographicsResponse.empty) { response, binding ->
            binding.read(
                details = details,
                response = response,
            )
        }

    override fun differences(first: DemographicsResponse, second: DemographicsResponse): Set<DemographicsField> =
        bindings
            .filter { it.differs(first = first, second = second) }
            .mapTo(mutableSetOf()) { it.field }

    override fun merged(
        target: DemographicsResponse,
        source: DemographicsResponse,
        fields: Set<DemographicsField>,
    ): DemographicsResponse = bindings
        .filter { it.field in fields }
        .fold(target) { response, binding ->
            binding.copied(
                source = source,
                target = response,
            )
        }

    override fun changes(baseline: DemographicsResponse, response: DemographicsResponse): Result<AccountModifications> {
        val modified = AccountDetails()
        val removed = AccountDetails()
        bindings.forEach {
            it.write(
                baseline = baseline,
                response = response,
                modified = modified,
                removed = removed,
            )
        }
        return AccountModifications(
            modifiedDetails = modified,
            removedAccountDetails = removed,
        )
    }

    /**
     * Ties the answer to [field] to the account value stored under [key].
     */
    private class Binding<T : Any>(
        val field: DemographicsField,
        val key: KnowledgeSourceType<AccountAnchor, T>,
        val get: (DemographicsResponse) -> T?,
        val set: (DemographicsResponse, T?) -> DemographicsResponse,
    ) {

        fun read(details: AccountDetails, response: DemographicsResponse) = set(response, details[key])

        fun differs(first: DemographicsResponse, second: DemographicsResponse) = get(first) != get(second)

        fun copied(source: DemographicsResponse, target: DemographicsResponse) = set(target, get(source))

        fun write(
            baseline: DemographicsResponse,
            response: DemographicsResponse,
            modified: AccountDetails,
            removed: AccountDetails,
        ) {
            val before = get(baseline)
            val after = get(response)
            when {
                before == after -> Unit
                after == null -> removed[key] = before
                else -> modified[key] = after
            }
        }
    }
}
