//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.requireDependency
import org.grovealliance.firebase.FirebaseAppConfiguration
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises reconfiguring an already initialized Firebase app, which needs a real Android runtime.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseAppConfigurationTest {

    private val configuration: FirebaseAppConfiguration get() = requireDependency()

    @Before
    fun initialize() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
    }

    @Test
    fun reusesTheAppAlreadyConfiguredForTheSameProject() {
        val app = FirebaseApp.getInstance()

        val result = configuration.configure(options = app.options)

        assertThat(result.getOrNull()).isSameInstanceAs(app)
    }

    @Test
    fun refusesToReportAnotherProjectAsConfigured() {
        val app = FirebaseApp.getInstance()
        val otherProject = FirebaseOptions.Builder(app.options).setProjectId(OTHER_PROJECT_ID).build()

        val result = configuration.configure(options = otherProject)

        assertThat(result.isFailure).isTrue()
        assertThat(configuration.isConfigured.value).isTrue()
        assertThat(FirebaseApp.getInstance().options.projectId).isEqualTo(app.options.projectId)
    }

    private companion object {
        const val OTHER_PROJECT_ID = "another-project"
    }
}
