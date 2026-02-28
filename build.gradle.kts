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
        jvmTarget = JvmTarget.JVM_11
        languageVersion = KotlinVersion.KOTLIN_2_1
    }
}

tasks.compileTestKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        languageVersion = KotlinVersion.DEFAULT
    }
}

kotlin {
    jvmToolchain(11)
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
        if (System.getenv().containsKey("MAVEN_REPO_URL")) {
            maven(System.getenv("MAVEN_REPO_URL")) {
                name = "reposilite"
                credentials {
                    username = System.getenv("MAVEN_REPO_USERNAME")
                        ?: project.findProperty("reposiliteUser") as String?
                    password = System.getenv("MAVEN_REPO_PASSWORD")
                        ?: project.findProperty("reposilitePassword") as String?
                }
            }
        }
    }
}
