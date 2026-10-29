//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.BuildConfig
import edu.stanford.myheartcounts.study.FakeStudyVariantProvider
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.studydefinition.StudyBundle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

/**
 * Reads the score explainer from the study bundle the app ships.
 */
class HeartHealthExplainerSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val variantProvider = FakeStudyVariantProvider()

    private val bundle: StudyBundle by lazy {
        val archive = File(GENERATED_ROOT)
            .walkTopDown()
            .firstOrNull { it.isFile && it.name == BuildConfig.STUDY_BUNDLE_ASSET_PATH }
            ?: error("No study bundle under $GENERATED_ROOT — run ./gradlew :myheartcounts:exportStudyBundle")
        val target = File(temporaryFolder.root, archive.name.removeSuffix(ARCHIVE_SUFFIX))
        archive.inputStream().use { StudyBundle.unpack(archive = it, bundleDir = target) }
        StudyBundle.open(target)
    }

    @Test
    fun `reads the explainer in English`() = runTest {
        // given
        variantProvider.setContentLocale(locale = Locale.US)

        // when
        val explainer = source(bundle = Result.success(bundle)).scoreExplainer()

        // then
        assertThat(explainer?.text()).startsWith("#### What is this score?")
    }

    @Test
    fun `reads the explainer in the study's content locale`() = runTest {
        // given
        variantProvider.setContentLocale(locale = Locale.forLanguageTag("es-US"))

        // when
        val explainer = source(bundle = Result.success(bundle)).scoreExplainer()

        // then
        assertThat(explainer?.text()).startsWith("#### ¿Qué es esta puntuación?")
    }

    @Test
    fun `has no explainer when the study bundle cannot be loaded`() = runTest {
        // when
        val explainer = source(bundle = Result.failure(IllegalStateException("unavailable"))).scoreExplainer()

        // then
        assertThat(explainer).isNull()
    }

    private fun source(bundle: Result<StudyBundle>) = HeartHealthExplainerSourceImpl(
        studyBundleProvider = object : MHCStudyBundleProvider {
            override suspend fun get(): Result<StudyBundle> = bundle
        },
        variantProvider = variantProvider,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun MarkdownDocument.text(): String =
        blocks.filterIsInstance<MarkdownBlock.Markdown>().joinToString(separator = "\n") { it.rawContents }

    private companion object {
        /**
         * Root the build exports the bundle archive beneath, relative to the module directory.
         */
        const val GENERATED_ROOT = "build/generated"
        const val ARCHIVE_SUFFIX = ".tar.zst"
    }
}
