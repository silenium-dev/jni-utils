import org.gradle.kotlin.dsl.signing
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jreleaser.gradle.plugin.JReleaserPlugin
import org.jreleaser.model.Active
import org.jreleaser.model.Signing
import kotlin.io.encoding.Base64

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.jreleaser)
    `maven-publish`
    signing
}

val nexusEnabled = (findProperty("nexus.enabled") as String?)?.toBoolean() ?: false
val mavenCentralEnabled = (findProperty("maven-central.enabled") as String?)?.toBoolean() ?: false
val signingEnabled = (findProperty("gpg.enabled") as String?)?.toBoolean() ?: false
val signingSecretKey = Base64.decode((findProperty("gpg.secret-key") as? String ?: "").trim()).decodeToString()
val signingPublicKey = Base64.decode((findProperty("gpg.public-key") as? String ?: "").trim()).decodeToString()
val signingPassphrase = (findProperty("gpg.passphrase") as? String ?: "").trim()
val stagingRepo = layout.buildDirectory.dir("m2-staging")

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()
    apply<JReleaserPlugin>()

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
            if (nexusEnabled) {
                val url = findProperty("nexus.repo-url") as? String ?: error("No deploy.repo-url specified")
                maven(url) {
                    name = "nexus"
                    credentials {
                        username = findProperty("nexus.username") as? String ?: ""
                        password = findProperty("nexus.password") as? String ?: ""
                    }
                }
            }
            maven {
                name = "staging"
                url = uri(stagingRepo)
            }
        }
        publications.withType<MavenPublication>() {
            pom {
                description = "A library for working with native libraries"
                url = "https://github.com/silenium-dev/jni-utils"
                inceptionYear = "2024"
                licenses {
                    license {
                        name = "AGPL-3.0-or-later"
                        url = "https://spdx.org/licenses/AGPL-3.0-or-later.html"
                    }
                }
                developers {
                    developer {
                        id = "silenium-dev"
                        email = "support@silenium-dev.net"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/silenium-dev/jni-utils.git"
                    developerConnection = "scm:git:ssh://github.com/silenium-dev/jni-utils.git"
                    url = "https://github.com/silenium-dev/jni-utils"
                }
            }
        }
    }

    jreleaser {
        signing {
            active = if (signingEnabled) Active.ALWAYS else Active.NEVER
            pgp {
                mode = Signing.Mode.MEMORY
                passphrase = signingPassphrase
                secretKey = signingSecretKey
                publicKey = signingPublicKey
            }
        }
        deploy {
            maven {
                mavenCentral {
                    register("sonatype") {
                        active = if (mavenCentralEnabled) Active.ALWAYS else Active.NEVER
                        url = "https://central.sonatype.com/api/v1/publisher"
                        stagingRepository(stagingRepo.get())
                        username = findProperty("maven-central.username") as? String ?: ""
                        password = findProperty("maven-central.password") as? String ?: ""
                    }
                }
                pomchecker {
                    strict = true
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
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}
