package dev.silenium.libs.jni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

@Suppress("unused") // used as plugin entrypoint
class NixNativesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
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
