rootProject.name = "Conditia"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}

plugins {
    // Downloads the Java 21 toolchain when the machine has none.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}

include(":conditia-api")
include(":conditia-common")
include(":conditia-adapter-configura")
include(":conditia-adapter-chassis")
include(":conditia-testing")
