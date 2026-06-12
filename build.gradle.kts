import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.io.encoding.Base64

plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
    signing
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false
val mavenCentralEnabled = (findProperty("maven-central.enabled") as String?)?.toBoolean() ?: false
val signingEnabled = (findProperty("gpg.enabled") as String?)?.toBoolean() ?: false

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()
    if (signingEnabled) apply<SigningPlugin>()

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
            if (mavenCentralEnabled) {
                mavenCentral {
                    credentials {
                        username = findProperty("maven-central.username") as? String ?: ""
                        password = findProperty("maven-central.password") as? String ?: ""
                    }
                }
            }
        }
    }

    if (signingEnabled) {
        signing {
            val secretKey = Base64.decode((findProperty("gpg.secret-key") as? String ?: "").trim()).decodeToString()
            val passphrase = (findProperty("gpg.passphrase") as? String ?: "").trim()
            useInMemoryPgpKeys(secretKey, passphrase)
            sign(publishing.publications)
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
