pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.nariko.org/release") }
    }
}
rootProject.name = "LightNovelReader-T2S-Plugin"
include(":converter", ":plugin", ":plugin-api3")
