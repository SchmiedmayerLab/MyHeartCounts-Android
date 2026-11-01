//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import com.google.firebase.FirebaseApp
import edu.stanford.myheartcounts.account.lastSignedConsentVersion
import edu.stanford.myheartcounts.standard.MHCAppRevision
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.firebase.FirebaseAppConfiguration
import org.grovealliance.study.StudyManager

/**
 * Details about the installed app, the backend it talks to and the participant's study state, for
 * support and diagnostics. A value is `null` when it is not available.
 */
data class DebugAppInfo(
    val version: String,
    val build: String,
    val firebaseProjectId: String?,
    val accountId: String?,
    val enrolledStudyRevision: String?,
    val bundledStudyRevision: String?,
    val lastSignedConsentVersion: String?,
)

/**
 * Provides the current [DebugAppInfo], updated as it changes.
 */
interface DebugAppInfoSource {

    /**
     * The app info, emitted again whenever the signed-in account, the study enrollments or the
     * Firebase setup change.
     */
    val info: Flow<DebugAppInfo>
}

/**
 * Default [DebugAppInfoSource] implementation.
 */
class DebugAppInfoSourceImpl(
    private val appRevision: MHCAppRevision,
    private val account: Account,
    private val studyManager: StudyManager,
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val firebaseAppConfiguration: FirebaseAppConfiguration,
) : DebugAppInfoSource {

    override val info: Flow<DebugAppInfo> = combine(
        account.details,
        studyManager.enrollments,
        firebaseAppConfiguration.isConfigured,
        flow { emit(studyBundleProvider.get().getOrNull()?.studyDefinition?.studyRevision?.toString()) },
    ) { details, enrollments, isFirebaseConfigured, bundledStudyRevision ->
        DebugAppInfo(
            version = appRevision.version,
            build = appRevision.build.toString(),
            firebaseProjectId = if (isFirebaseConfigured) firebaseProjectId() else null,
            accountId = details?.accountId,
            enrolledStudyRevision = enrollments.firstOrNull()?.studyRevision?.toString(),
            bundledStudyRevision = bundledStudyRevision,
            lastSignedConsentVersion = details?.lastSignedConsentVersion,
        )
    }

    private fun firebaseProjectId(): String? = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull()
}
