plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":domain"))

    testImplementation(kotlin("test"))
}
