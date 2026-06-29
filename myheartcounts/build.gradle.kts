plugins {
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.spezi.application)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.serialization)
}

android {
    namespace = "edu.stanford.myheartcounts"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "edu.stanford.myheartcounts"
        versionCode = 1
        versionName = "1.0.0"
        targetSdk = libs.versions.targetSdk.get().toInt()

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":consent"))
    implementation(project(":core"))
    implementation(project(":core-viewmodel"))
    implementation(project(":ui"))
    implementation(project(":onboarding"))
    implementation(project(":account"))
    implementation(project(":storage-local"))

    implementation(libs.bundles.navigation3)

    testImplementation(project(":testing-screenshot"))
}
