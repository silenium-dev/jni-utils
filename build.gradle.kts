import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

group = "dev.silenium.libs.jni"
version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.commons.lang3)
    implementation(libs.jetbrains.annotations)

    testImplementation(libs.logback.classic)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        languageVersion = KotlinVersion.KOTLIN_1_7
    }
}

tasks.compileTestKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        languageVersion = KotlinVersion.DEFAULT
    }
}

kotlin {
    jvmToolchain(8)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven(System.getenv("REPOSILITE_URL") ?: "https://reposilite.silenium.dev/snapshots") {
            name = "reposilite"
            credentials {
                username = System.getenv("REPOSILITE_USERNAME") ?: project.findProperty("reposiliteUser") as String?
                        ?: ""
                password = System.getenv("REPOSILITE_PASSWORD") ?: project.findProperty("reposilitePassword") as String?
                        ?: ""
            }
        }
    }
}
