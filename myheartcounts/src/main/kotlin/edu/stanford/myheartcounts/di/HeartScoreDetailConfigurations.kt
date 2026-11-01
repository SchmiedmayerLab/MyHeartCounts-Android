//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import android.app.Application
import edu.stanford.myheartcounts.customsamples.CustomSampleUploader
import edu.stanford.myheartcounts.customsamples.CustomSampleUploaderImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreBandLabels
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreBandLabelsImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreBandMapper
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreBandMapperImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreChartMapper
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreChartMapperImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreChartSource
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreChartSourceImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDebugSource
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDebugSourceImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDetailContentMapper
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDetailContentMapperImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDetailRanges
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDetailRangesImpl
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreExplainerSource
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreExplainerSourceImpl
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreBmiStore
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreBmiStoreImpl
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntryContentMapper
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntryContentMapperImpl
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntryWriter
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntryWriterImpl
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreRecordMapper
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreRecordStore
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreRecordStoreImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.coroutines.Concurrency

/**
 * Registers the sources and layout mappers of a heart score's details.
 */
fun ConfigurationBuilder.heartScoreDetailConfigurations() {
    heartScoreEntryConfigurations()
    factory<HeartScoreDetailRanges> {
        HeartScoreDetailRangesImpl(
            timeProvider = dependency(),
            calendarChanges = dependency(),
        )
    }
    factory<HeartScoreChartSource> {
        HeartScoreChartSourceImpl(
            stats = dependency(),
            customSamples = dependency(),
            health = dependency(),
            account = dependency(),
            ranges = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<HeartScoreExplainerSource> {
        HeartScoreExplainerSourceImpl(
            studyBundleProvider = dependency(),
            variantProvider = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }
    factory<HeartScoreDebugSource> {
        HeartScoreDebugSourceImpl(
            account = dependency(),
            customSamples = dependency(),
        )
    }
    factory<HeartScoreBandLabels> { HeartScoreBandLabelsImpl() }
    factory<HeartScoreBandMapper> { HeartScoreBandMapperImpl(labels = dependency()) }
    factory<HeartScoreChartMapper> {
        HeartScoreChartMapperImpl(
            timeProvider = dependency(),
            ranges = dependency(),
        )
    }
    factory<HeartScoreDetailContentMapper> {
        HeartScoreDetailContentMapperImpl(
            dateFormatter = dependency(),
            bandMapper = dependency(),
        )
    }
}

private fun ConfigurationBuilder.heartScoreEntryConfigurations() {
    factory<HeartScoreEntryContentMapper> { HeartScoreEntryContentMapperImpl(timeProvider = dependency()) }
    singleton<CustomSampleUploader> {
        CustomSampleUploaderImpl(
            firestore = dependency(),
            appRevision = dependency(),
            studyManager = dependency(),
            timeProvider = dependency(),
        )
    }
    factory { HeartScoreRecordMapper(timeProvider = dependency()) }
    factory<HeartScoreRecordStore> { HeartScoreRecordStoreImpl(
        application = appContext() as Application,
        mapper = dependency(),
    ) }
    factory<HeartScoreBmiStore> { HeartScoreBmiStoreImpl(
        firestore = dependency(),
        timeProvider = dependency(),
    ) }
    factory<HeartScoreEntryWriter> {
        HeartScoreEntryWriterImpl(
            records = dependency(), custom = dependency(), bmiStore = dependency(),
            statsSync = dependency(), scores = dependency(),
        )
    }
}
