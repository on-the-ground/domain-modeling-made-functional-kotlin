plugins {
	kotlin("jvm") version "2.0.20"
	kotlin("kapt") version "2.0.20"
}

group = "org.ontheground"
version = "0.0.1"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}


val kotestVersion = "5.5.4"
val arrowVersion = "1.2.4"
dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.arrow-kt:arrow-core:$arrowVersion")
	implementation("io.arrow-kt:arrow-optics:$arrowVersion")
	kapt("io.arrow-kt:arrow-meta:$arrowVersion")

	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
	testImplementation("io.kotest:kotest-framework-api:$kotestVersion")
	testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
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
