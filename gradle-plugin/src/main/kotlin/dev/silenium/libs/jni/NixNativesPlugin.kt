package dev.silenium.libs.jni

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

@Suppress("unused") // used as plugin entrypoint
class NixNativesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<KotlinPluginWrapper>()
        target.configure<KotlinJvmExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        }
        target.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        val ext = target.extensions.create("nixNatives", NixNativesExtension::class.java)
        ext.libVersion.convention(target.version.toString())
        ext.libName.convention(target.name)
        ext.nixFlakeLock.convention(target.layout.file(ext.nixFlake.map { it.asFile.resolveSibling("flake.lock") }))
        val nixResultDir = target.layout.buildDirectory.dir("nix-result")

        val nixClean = target.tasks.register<Delete>("nixClean") {
            delete(nixResultDir)
        }
        target.tasks.named("clean").configure {
            it.dependsOn(nixClean)
        }

        val nixBuild = target.tasks.register<NixBuildTask>("nixBuild") {
            doFirst {
                nixResultDir.get().asFile.deleteRecursively()
            }
            group = "build"
            inputs.files(ext.sourceFiles)
            inputs.files(ext.nixFlake, ext.nixFlakeLock)
            libName.set(ext.libName)
            resultDir.set(nixResultDir)
            showLogs.set(ext.showLogs)
        }

        target.afterEvaluate { project ->
            project.tasks.named<ProcessResources>("processResources") {
                val out = nixBuild.flatMap { it.resultDir.asFile }
                from(out)
                dirPermissions {
                    it.unix("755")
                }
            }
        }
    }
}
