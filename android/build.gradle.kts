import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            source.setFrom(
                "src/main/kotlin",
                "src/main/java",
                "src/test/kotlin",
                "src/androidTest/kotlin",
            )
        }
        // detekt 1.23 derives the JVM target from the daemon JDK when unset; Android Studio may run Gradle on JDK 25.
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "17"
        }
    }
}
