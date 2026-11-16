//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import edu.stanford.myheartcounts.consentforms.SignedConsentFormSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.requireDependency
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Reads back consent forms from the Cloud Storage emulator: one stored the way every platform
 * stores a signed form, and one stored without any metadata.
 */
@RunWith(AndroidJUnit4::class)
class SignedConsentFormSourceTest {

    private val source: SignedConsentFormSource get() = requireDependency()

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun signOut() = runTest {
        FirebaseTestSession.signOut()
    }

    @Test
    fun listsTheFormsNewestFirstWithTheirStoredDetails() = runTest {
        // given
        store(
            name = OLDER,
            metadata = mapOf(
                "consentFormMetadata" to """{"title":"My Heart Counts Consent Form","version":"1.0.0"}""",
                "date" to "2026-01-01T09:00:00Z",
                "version" to "1.0.0",
            ),
        )
        store(
            name = NEWER,
            metadata = emptyMap(),
        )

        // when
        val forms = source.forms().getOrThrow()

        // then
        assertThat(forms.map { it.id }).containsExactly(NEWER, OLDER).inOrder()
        assertThat(forms.last().title).isEqualTo("My Heart Counts Consent Form")
        assertThat(forms.last().version).isEqualTo("1.0.0")
        assertThat(forms.last().signedAt).isEqualTo(Instant.parse("2026-01-01T09:00:00Z"))
        assertThat(forms.first().title).isNull()
        assertThat(forms.first().signedAt).isNull()
    }

    @Test
    fun downloadsAFormAndDiscardsItAgain() = runTest {
        // given
        store(
            name = OLDER,
            metadata = emptyMap(),
        )
        val form = source.forms().getOrThrow().single()

        // when
        val download = source.download(form = form).getOrThrow()

        // then
        assertThat(download.file.readBytes()).isEqualTo(PDF)
        assertThat(download.contentUri).startsWith("content://")

        // when
        source.discard(download = download)

        // then
        assertThat(download.file.exists()).isFalse()
    }

    private suspend fun store(
        name: String,
        metadata: Map<String, String>,
    ) {
        FirebaseStorage.getInstance().reference
            .child("users/${FirebaseTestSession.firestore.accountId()}/consent/$name")
            .putBytes(
                PDF,
                storageMetadata {
                    contentType = "application/pdf"
                    metadata.forEach { (key, value) -> setCustomMetadata(key, value) }
                },
            )
            .await()
    }

    private companion object {
        const val OLDER = "1767258000.pdf"
        const val NEWER = "1767344400.pdf"
        val PDF = "%PDF-1.4\n%%EOF\n".toByteArray()
    }
}
