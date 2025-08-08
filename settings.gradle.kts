rootProject.name = "dmmf"

pluginManagement {
    repositories {
        gradlePluginPortal() // ← KSP 플러그인 여기서 받아옴
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
    }
}