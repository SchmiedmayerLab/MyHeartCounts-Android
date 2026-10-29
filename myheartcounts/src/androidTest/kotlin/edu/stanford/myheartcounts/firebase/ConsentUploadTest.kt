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
import edu.stanford.myheartcounts.account.didOptInToTrial
import edu.stanford.myheartcounts.account.futureStudies
import edu.stanford.myheartcounts.account.lastSignedConsentDate
import edu.stanford.myheartcounts.account.lastSignedConsentVersion
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.standard.consent.MHCConsentHandler
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.consent.SignatureMetadata
import org.grovealliance.consent.SignaturePoint
import org.grovealliance.consent.SignatureStroke
import org.grovealliance.core.requireDependency
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the signed consent form end to end: the study's real consent markdown is rendered to a
 * PDF, uploaded to Cloud Storage, and the account is stamped.
 *
 * The PDF renderer has no unit coverage worth the name — it draws to a `Canvas` — so this is what
 * establishes that it produces a real file rather than throwing or emitting an empty document.
 */
@RunWith(AndroidJUnit4::class)
class ConsentUploadTest {

    private val consentHandler: MHCConsentHandler get() = requireDependency()

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
    fun uploadsARenderedConsentPdfAndStampsTheAccount() = runTest {
        consentHandler.upload(responses = responses()).getOrThrow()

        val consentFolder = FirebaseStorage.getInstance().reference
            .child("users/${FirebaseTestSession.firestore.accountId()}/consent")
        val uploaded = consentFolder.listAll().await().items
        assertThat(uploaded).hasSize(1)

        val metadata = uploaded.single().metadata.await()
        assertThat(metadata.contentType).isEqualTo("application/pdf")
        // A PDF header alone would fit in a handful of bytes; a real rendered document does not.
        assertThat(metadata.sizeBytes).isGreaterThan(MINIMUM_PLAUSIBLE_PDF_BYTES)

        // The four custom metadata keys the study reads the form back with.
        assertThat(metadata.getCustomMetadata("consentFormMetadata")).isNotNull()
        assertThat(metadata.getCustomMetadata("responses")).contains(SIGNATURE_ELEMENT_ID)
        assertThat(metadata.getCustomMetadata("date")).isNotNull()
        assertThat(metadata.getCustomMetadata("version")).isEqualTo(CONSENT_VERSION)

        // The filename is a unix timestamp, which is what the read-back listing sorts on.
        assertThat(uploaded.single().name).matches("""\d+\.pdf""")

        // Stamped by a separate account update, which reaches the account through its snapshot
        // listener some time after the upload returned.
        val stamped = FirebaseTestSession.awaitDetails { it?.lastSignedConsentDate != null }
        assertThat(stamped?.lastSignedConsentDate).isNotNull()
        assertThat(stamped?.lastSignedConsentVersion).isEqualTo(CONSENT_VERSION)
        assertThat(stamped?.futureStudies).isTrue()
        assertThat(stamped?.didOptInToTrial).isTrue()
    }

    private fun responses() = ConsentResponses(
        toggles = mapOf(MHCConsentDocumentProvider.ID_FUTURE_STUDIES to true),
        selects = mapOf(MHCConsentDocumentProvider.ID_TRIAL_OPT_IN to MHCConsentDocumentProvider.ID_TRIAL_YES),
        signatures = mapOf(
            SIGNATURE_ELEMENT_ID to SignatureMetadata(
                givenName = "Test",
                familyName = "Participant",
                strokes = listOf(
                    SignatureStroke(
                        points = listOf(
                            SignaturePoint(x = 0f, y = 0f),
                            SignaturePoint(x = 40f, y = 25f),
                            SignaturePoint(x = 80f, y = 5f),
                        ),
                    ),
                ),
            ),
        ),
    )

    private companion object {
        const val SIGNATURE_ELEMENT_ID = "participant-signature"
        const val MINIMUM_PLAUSIBLE_PDF_BYTES = 1_000L
        const val CONSENT_VERSION = "0.1.0"
    }
}
