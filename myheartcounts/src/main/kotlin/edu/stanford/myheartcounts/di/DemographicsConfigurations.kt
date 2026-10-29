//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.demographics.DemographicsAnswerMapper
import edu.stanford.myheartcounts.demographics.DemographicsAnswerMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsAutofill
import edu.stanford.myheartcounts.demographics.DemographicsAutofillImpl
import edu.stanford.myheartcounts.demographics.DemographicsCatalog
import edu.stanford.myheartcounts.demographics.DemographicsCatalogImpl
import edu.stanford.myheartcounts.demographics.DemographicsChoiceGroupMapper
import edu.stanford.myheartcounts.demographics.DemographicsChoiceGroupMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsChoiceSheetMapper
import edu.stanford.myheartcounts.demographics.DemographicsChoiceSheetMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsCodec
import edu.stanford.myheartcounts.demographics.DemographicsCodecImpl
import edu.stanford.myheartcounts.demographics.DemographicsConditionsProvider
import edu.stanford.myheartcounts.demographics.DemographicsConditionsProviderImpl
import edu.stanford.myheartcounts.demographics.DemographicsEditor
import edu.stanford.myheartcounts.demographics.DemographicsEditorImpl
import edu.stanford.myheartcounts.demographics.DemographicsLayoutMapper
import edu.stanford.myheartcounts.demographics.DemographicsLayoutMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsOptionIdMapper
import edu.stanford.myheartcounts.demographics.DemographicsOptionIdMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsOptionsProvider
import edu.stanford.myheartcounts.demographics.DemographicsOptionsProviderImpl
import edu.stanford.myheartcounts.demographics.DemographicsPickerMapper
import edu.stanford.myheartcounts.demographics.DemographicsPickerMapperImpl
import edu.stanford.myheartcounts.demographics.DemographicsRegionSource
import edu.stanford.myheartcounts.demographics.DemographicsRegionSourceImpl
import edu.stanford.myheartcounts.demographics.DemographicsRegionsProvider
import edu.stanford.myheartcounts.demographics.DemographicsRegionsProviderImpl
import edu.stanford.myheartcounts.demographics.DemographicsRules
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.coroutines.Concurrency

/**
 * Registers the shared demographics editor and its presentation services.
 */
fun ConfigurationBuilder.demographicsConfigurations() {
    singleton<DemographicsOptionsProvider> { DemographicsOptionsProviderImpl() }
    singleton<DemographicsRegionsProvider> { DemographicsRegionsProviderImpl() }
    singleton<DemographicsConditionsProvider> { DemographicsConditionsProviderImpl() }
    singleton<DemographicsCatalog> {
        DemographicsCatalogImpl(
            options = dependency(),
            regions = dependency(),
            conditions = dependency(),
        )
    }
    singleton<DemographicsOptionIdMapper> { DemographicsOptionIdMapperImpl() }
    singleton<DemographicsAnswerMapper> { DemographicsAnswerMapperImpl(optionIdMapper = dependency()) }
    singleton<DemographicsCodec> { DemographicsCodecImpl(timeProvider = dependency()) }
    singleton { DemographicsRules() }
    singleton<DemographicsRegionSource> {
        DemographicsRegionSourceImpl(variantProvider = dependency())
    }
    module<DemographicsEditor> {
        DemographicsEditorImpl(
            account = dependency(),
            accountService = dependency(),
            codec = dependency(),
            rules = dependency(),
            regionSource = dependency(),
            scope = dependency<Concurrency>().mainImmediateCoroutineScope(),
        )
    }
    singleton<DemographicsLayoutMapper> {
        DemographicsLayoutMapperImpl(
            catalog = dependency(),
            optionIdMapper = dependency(),
            answerMapper = dependency(),
            rules = dependency(),
        )
    }
    singleton<DemographicsPickerMapper> {
        DemographicsPickerMapperImpl(
            answerMapper = dependency(),
            timeProvider = dependency(),
        )
    }
    singleton<DemographicsChoiceGroupMapper> { DemographicsChoiceGroupMapperImpl(catalog = dependency()) }
    singleton<DemographicsChoiceSheetMapper> {
        DemographicsChoiceSheetMapperImpl(
            catalog = dependency(),
            optionIdMapper = dependency(),
            groupMapper = dependency(),
            rules = dependency(),
            timeProvider = dependency(),
        )
    }
    singleton<DemographicsAutofill> {
        DemographicsAutofillImpl(
            health = dependency(),
            healthAccess = dependency(),
        )
    }
}
