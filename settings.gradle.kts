pluginManagement {

    includeBuild("build-logic")

    plugins {
        `kotlin-dsl`
    }

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }


}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MetaRadar"
include(":app")
