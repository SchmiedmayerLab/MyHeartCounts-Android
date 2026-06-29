plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
}

android {
    namespace = "edu.stanford.spezi.onboarding"
}

dependencies {
    implementation(project(":ui"))
}
