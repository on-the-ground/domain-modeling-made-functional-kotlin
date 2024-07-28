import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.ontheground"
version = "1.0-SNAPSHOT"

@Suppress("DSL_SCOPE_VIOLATION") plugins {
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.power.assert)
}



repositories {
    mavenCentral()
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }

    test {
        useJUnitPlatform()
    }
}

ktor {
    docker {
        jreVersion = JavaVersion.VERSION_19
        localImageName = "ktor-arrow-example"
        imageTag = "latest"
    }
}

spotless {
    kotlin {
        targetExclude("**/build/**")
        ktfmt("0.46").googleStyle()
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.suspendapp)
    implementation(libs.kjwt.core)
    implementation(libs.logback.classic)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.slugify)
    implementation(libs.bundles.cohort)

    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.bundles.kotest)
    implementation(kotlin("stdlib-jdk8"))
}
