plugins {
    id("buildlogic.kotlin-common-conventions")
}

group = "org.ontheground"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("script-runtime"))
//    implementation(libs.arrow.core)
//    implementation(libs.arrow.fx)
//    testImplementation(kotlin("test")) // The Kotlin test library
}


tasks.test {
    useJUnitPlatform()
}
