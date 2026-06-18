pluginManagement {
    repositories {
        maven("https://nexus.silenium.dev/repository/maven-releases/")
        maven("https://nexus.silenium.dev/repository/maven-snapshots/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "jni-utils"

include(":gradle-plugin", ":jni-utils")
