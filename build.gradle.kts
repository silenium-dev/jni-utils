import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    group = "dev.silenium.libs.jni"
    val gitVersionProvider = providers.gradleProperty("ci").flatMap {
        if (it.toBoolean()) {
            providers.exec {
                commandLine("git", "describe", "--tags", "--always", "--dirty", "--abbrev=8")
                workingDir = layout.projectDirectory.asFile
            }.standardOutput.asText.map(String::trim)
        } else null
    }
    version = providers
        .gradleProperty("deploy.version")
        .orElse(gitVersionProvider)
        .orElse("0.0.0-SNAPSHOT")
        .get()

    repositories {
        mavenCentral()
    }

    publishing {
        repositories {
            if (deployEnabled) {
                val url = findProperty("deploy.repo-url") as? String ?: error("No deploy.repo-url specified")
                maven(url) {
                    name = "nexus"
                    credentials {
                        username = findProperty("deploy.username") as? String ?: ""
                        password = findProperty("deploy.password") as? String ?: ""
                    }
                }
            }
        }
    }
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
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}
