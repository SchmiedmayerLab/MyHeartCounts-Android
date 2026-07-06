plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
}

android {
    namespace = "edu.stanford.spezi.consent"
}

dependencies {
    api(project(":core"))
    api(project(":ui"))
    implementation(project(":core-time"))
    implementation(project(":core-viewmodel"))
    implementation(project(":markdown"))

    testImplementation(project(":testing-core"))
    testImplementation(project(":testing-screenshot"))
}
