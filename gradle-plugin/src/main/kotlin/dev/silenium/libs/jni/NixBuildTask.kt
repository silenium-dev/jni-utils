package dev.silenium.libs.jni

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Nix build outputs are read-only, which breaks gradle caching")
abstract class NixBuildTask : Exec() {
    @get:Input
    abstract val libName: Property<String>

    @get:Input
    abstract val showLogs: Property<Boolean>

    @get:OutputDirectory
    abstract val resultDir: DirectoryProperty

    @get:Input
    abstract val flakeDir: Property<File>

    override fun exec() {
        val logArgs = arrayOf("--print-build-logs").takeIf { showLogs.get() } ?: arrayOf()
        commandLine("nix", "build", "-o", resultDir.get().asFile.absolutePath, *logArgs, ".#${libName.get()}")
        standardOutput = System.out
        errorOutput = System.out
        workingDir = flakeDir.get()
        super.exec()
    }
}
