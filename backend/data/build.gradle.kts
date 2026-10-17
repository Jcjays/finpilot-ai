plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    `java-test-fixtures`
}

dependencies {
    implementation(project(":domain"))

    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.kotlin.datetime)
    api(libs.exposed.json)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // Shared integration-test database helper, consumed by other modules via testFixtures(project(":data")).
    testFixturesImplementation(libs.hikaricp)
    testFixturesImplementation(libs.flyway.core)
    testFixturesImplementation(libs.flyway.postgresql)
    testFixturesImplementation(libs.postgresql)
    testFixturesApi(libs.testcontainers.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.testcontainers.junit)
    testRuntimeOnly(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    // Pass the CI / local database coordinates through to the tests; Testcontainers is the fallback.
    listOf("TEST_DATABASE_URL", "TEST_DATABASE_USER", "TEST_DATABASE_PASSWORD").forEach { name ->
        System.getenv(name)?.let { environment(name, it) }
    }
}
