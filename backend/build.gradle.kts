import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt) apply false
}

/**
 * Integration tests in several modules share one PostgreSQL database and each test class recreates the
 * schema, so the test tasks must not run concurrently. A build service with a single permit serialises them.
 */
abstract class IntegrationDatabaseLock : BuildService<BuildServiceParameters.None>

val integrationDatabaseLock = gradle.sharedServices.registerIfAbsent(
    "integrationDatabaseLock",
    IntegrationDatabaseLock::class,
) {
    maxParallelUsages.set(1)
}

subprojects {
    group = "com.finpilot"
    version = "0.1.0"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            usesService(integrationDatabaseLock)
            testLogging {
                events("passed", "failed", "skipped")
                showStandardStreams = false
            }
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        }
    }
}
