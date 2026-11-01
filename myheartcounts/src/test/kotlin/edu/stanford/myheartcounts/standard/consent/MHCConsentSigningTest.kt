//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.BuildConfig
import edu.stanford.myheartcounts.account.LastSignedConsentVersionKey
import edu.stanford.myheartcounts.study.FakeStudyVariantProvider
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.grovealliance.studydefinition.StudyBundle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MHCConsentSigningTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val account = FakeAccount()

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
    fun `requires signing when the participant never signed`() = runTest {
        // given
        signIn(signedVersion = null)

        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isTrue()
    }

    @Test
    fun `requires signing when nobody is signed in`() = runTest {
        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isTrue()
    }

    @Test
    fun `does not require signing the version already signed`() = runTest {
        // given
        signIn(signedVersion = BUNDLED_CONSENT_VERSION)

        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isFalse()
    }

    @Test
    fun `requires signing when the minor version is newer`() = runTest {
        // given
        signIn(signedVersion = "0.0.9")

        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isTrue()
    }

    @Test
    fun `does not require signing when a newer version was signed`() = runTest {
        // given
        signIn(signedVersion = "1.0.0")

        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isFalse()
    }

    @Test
    fun `requires signing when the signed version cannot be read`() = runTest {
        // given
        signIn(signedVersion = "not a version")

        // when
        val isRequired = signing().isSigningRequired()

        // then
        assertThat(isRequired).isTrue()
    }

    @Test
    fun `requires signing when the consent document cannot be loaded`() = runTest {
        // given
        signIn(signedVersion = BUNDLED_CONSENT_VERSION)

        // when
        val isRequired = signing(bundle = Result.failure(IllegalStateException("missing"))).isSigningRequired()

        // then
        assertThat(isRequired).isTrue()
    }

    private fun signIn(signedVersion: String?) {
        account.supplyUserDetails(
            details = AccountDetails().apply {
                signedVersion?.let { this[LastSignedConsentVersionKey::class] = it }
            },
        )
    }

    private fun signing(bundle: Result<StudyBundle> = Result.success(this.bundle)) = MHCConsentSigningImpl(
        account = account,
        documentProvider = MHCConsentDocumentProvider(
            studyBundleProvider = object : MHCStudyBundleProvider {
                override suspend fun get(): Result<StudyBundle> = bundle
            },
            variantProvider = FakeStudyVariantProvider(),
        ),
    )

    private companion object {
        const val BUNDLED_CONSENT_VERSION = "0.1.0"
        const val GENERATED_ROOT = "build/generated"
        const val ARCHIVE_SUFFIX = ".tar.zst"
    }
}
