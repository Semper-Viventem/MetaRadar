enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {

    plugins {
        `kotlin-dsl`
    }

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MetaRadar"
include(":app")
