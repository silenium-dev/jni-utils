import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.conventions.jvm)
}

group = "dev.silenium.libs.jni"

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.commons.lang3)
    implementation(libs.jetbrains.annotations)

    testImplementation(libs.logback.classic)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

conventions {
    jvm {
        jvmTarget = JvmTarget.JVM_11
        kotlinTarget = KotlinVersion.KOTLIN_2_2
    }
    publishing {
        enabled = true
    }
}
