plugins {
    kotlin("jvm") version "2.0.0"
    `kotlin-dsl`
}

group = "org.ontheground"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("script-runtime"))
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx)
    testImplementation(kotlin("test")) // The Kotlin test library
}


tasks.test {
    useJUnitPlatform()
}
