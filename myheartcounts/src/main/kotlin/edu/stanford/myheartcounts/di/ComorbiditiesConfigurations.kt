//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProvider
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProviderImpl
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesMapper
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesMapperImpl
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesRules
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesStorage
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesStorageImpl
import edu.stanford.myheartcounts.comorbidities.DiagnosisDateMapper
import edu.stanford.myheartcounts.comorbidities.DiagnosisDateMapperImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.coroutines.Concurrency

/**
 * Registers the participant's comorbidities: the reportable conditions, their storage, rules and
 * presentation, shared with the demographics questions.
 */
fun ConfigurationBuilder.comorbiditiesConfigurations() {
    singleton<ComorbiditiesConditionsProvider> { ComorbiditiesConditionsProviderImpl() }
    singleton<ComorbiditiesStorage> {
        ComorbiditiesStorageImpl(
            account = dependency(),
            scope = dependency<Concurrency>().mainImmediateCoroutineScope(),
        )
    }
    singleton { ComorbiditiesRules() }
    singleton<DiagnosisDateMapper> {
        DiagnosisDateMapperImpl(
            conditionsProvider = dependency(),
            timeProvider = dependency(),
        )
    }
    singleton<ComorbiditiesMapper> {
        ComorbiditiesMapperImpl(
            conditionsProvider = dependency(),
            diagnosisDateMapper = dependency(),
            rules = dependency(),
        )
    }
}
