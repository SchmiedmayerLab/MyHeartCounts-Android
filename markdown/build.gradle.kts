plugins {
    alias(libs.plugins.spezi.library)
    alias(libs.plugins.spezi.compose)
    alias(libs.plugins.spezi.serialization)
}

android {
    namespace = "edu.stanford.spezi.markdown"
}

dependencies {
    api(project(":foundation"))
    implementation(project(":ui-theme"))
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":testing-core"))
    testImplementation(project(":ui"))
}
