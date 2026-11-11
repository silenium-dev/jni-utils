package dev.silenium.libs.jni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
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
            if (ext.processResources.getOrElse(true)) {
                project.tasks.withType<ProcessResources>().configureEach { task ->
                    if (task.name != "processResources") return@configureEach
                    val out = nixBuild.flatMap { it.resultDir.asFile }
                    task.from(out)
                    task.dirPermissions {
                        it.unix("755")
                    }
                }
            }
        }
    }
}
