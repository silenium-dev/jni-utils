import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.conventions.plugin)
}

dependencies {
    api(project(":jni-utils"))
    implementation(gradleKotlinDsl())
    implementation(gradleApi())

    testImplementation(libs.logback.classic)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

gradlePlugin {
    plugins {
        register("jni-utils") {
            id = "dev.silenium.libs.jni.nix-natives"
            implementationClass = "dev.silenium.libs.jni.NixNativesPlugin"
        }
    }
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
