
plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("kapt") version "2.0.21"
	kotlin("plugin.serialization") version "2.0.21"
}

group = "org.ontheground"
version = "0.0.1"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}


val kotestVersion = "5.9.1"
val arrowVersion = "1.2.4"
val exposedVersion = "0.56.0"
val postgresDriverVersion = "42.7.4"
dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
	implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
	implementation("io.arrow-kt:arrow-core:$arrowVersion")
	implementation("io.arrow-kt:arrow-optics:$arrowVersion")
	implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
	implementation("org.postgresql:postgresql:$postgresDriverVersion")

	kapt("io.arrow-kt:arrow-meta:1.6.2")

	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
	testImplementation("io.kotest:kotest-framework-api:$kotestVersion")
	testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xcontext-receivers")
		languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
		apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
	}
}

kapt {
	useBuildCache = false
}

tasks.test {
	useJUnitPlatform()
}
