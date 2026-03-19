plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
}

group = "dev.silenium.libs.jni"
version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
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

java {
    withSourcesJar()
}

gradlePlugin {
    plugins {
        register("jni-utils") {
            id = "dev.silenium.libs.jni.nix-natives"
            implementationClass = "dev.silenium.libs.jni.NixNativesPlugin"
        }
    }
}
