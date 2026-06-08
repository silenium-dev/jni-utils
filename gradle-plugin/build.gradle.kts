import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
}

dependencies {
    api(project(":"))
    api(libs.kotlin.jvm.gradle.plugin)
    implementation(gradleKotlinDsl())
    implementation(gradleApi())

    testImplementation(libs.logback.classic)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

gradlePlugin {
    plugins {
        register("jni-utils") {
            id = "dev.silenium.libs.jni.nix-natives"
            implementationClass = "dev.silenium.libs.jni.NixNativesPlugin"
        }
    }
}
