//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.mhc.studybundle)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "edu.stanford.myheartcounts"
    compileSdk = libs.versions.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId =
            (project.findProperty("android.injected.application.id") as? String)
                ?: "edu.stanford.myheartcounts"
        versionCode =
            (project.findProperty("android.injected.version.code") as? String)?.toInt() ?: 1
        versionName =
            (project.findProperty("android.injected.version.name") as? String)
                ?: providers.gradleProperty("app.versionName").get()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Host of a local Firebase emulator suite to talk to instead of a real project, empty to
        // use the real one. Opt-in rather than a debug default, because a debug build is also how
        // the app is run against myheart-counts-development. `10.0.2.2` is the host machine as seen
        // from an Android emulator.
        buildConfigField(
            "String",
            "FIREBASE_EMULATOR_HOST",
            "\"${(project.findProperty("mhc.firebaseEmulatorHost") as? String).orEmpty()}\"",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        // HAPI FHIR, which reaches the app through the questionnaire module, needs the desugared
        // java.time APIs.
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
            // Offline-instrumented classes cannot be read back by the Jacoco report task, which
            // reads the same class directories.
            enableAndroidTestCoverage = false
        }
    }
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.view.model.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.navigation.compose)
    implementation(libs.bundles.navigation3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.storage.ktx)

    coreLibraryDesugaring(libs.android.desugaring)

    implementation(libs.grove.account)
    implementation(libs.grove.account.firebase)
    implementation(libs.grove.consent)
    implementation(libs.grove.core)
    implementation(libs.grove.core.coroutines)
    implementation(libs.grove.core.lifecycle)
    implementation(libs.grove.core.logging)
    implementation(libs.grove.core.time)
    implementation(libs.grove.core.viewmodel)
    implementation(libs.grove.firebase)
    implementation(libs.grove.health)
    implementation(libs.grove.markdown)
    implementation(libs.grove.onboarding)
    implementation(libs.grove.questionnaire)
    implementation(libs.grove.scheduler)
    implementation(libs.grove.storage.local)
    implementation(libs.grove.study)
    implementation(libs.grove.study.definition)
    implementation(libs.grove.ui)
    implementation(libs.grove.ui.scheduler)

    // The AAR carries the on-device native libraries; the plain jar carries the desktop ones and is
    // what the unit tests below pull in.
    implementation(variantOf(libs.zstd.jni) { artifactType("aar") })

    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.integration.testing)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(testFixtures(libs.grove.study))

    testImplementation(libs.bundles.unit.testing)
    testImplementation(libs.grove.testing.screenshot)
    // The bundle archive is unpacked in a unit test, which needs the desktop native libraries the
    // plain jar carries rather than the on-device ones in the AAR.
    testImplementation(libs.zstd.jni)
    testImplementation(testFixtures(libs.grove.core.time))
    testImplementation(testFixtures(libs.grove.foundation))
    testImplementation(testFixtures(libs.grove.scheduler))
    testImplementation(testFixtures(libs.grove.study))
    testImplementation(testFixtures(libs.grove.study.definition))
}

// HAPI FHIR ships an older Guava than the rest of the graph resolves to; the questionnaire module
// pins it for the same reason.
configurations.configureEach {
    resolutionStrategy {
        force(libs.guava)
    }
}
