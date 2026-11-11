package dev.silenium.libs.jni

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import java.io.ByteArrayOutputStream

interface NixEvalSpec {
    val workingDir: DirectoryProperty
    val expression: Property<String>
}

fun Project.nixEval(config: Action<NixEvalSpec>): Provider<JsonElement> {
    val spec = objects.newInstance<NixEvalSpec>()
    config.execute(spec)
    val cmdLine = listOf("nix", "eval", "--json", spec.expression)
    val stdErr = ByteArrayOutputStream()
    val stdOut = ByteArrayOutputStream()
    val execResult = project.providers.exec {
        it.commandLine(cmdLine)
        it.workingDir(spec.workingDir)
        it.standardOutput = stdOut
        it.errorOutput = stdErr
    }
    return execResult.result.map { result ->
        if (result.exitValue != 0) {
            logger.error("Error running ${cmdLine.joinToString(" ")} with exit code ${result.exitValue}: ${stdErr.toString(Charsets.UTF_8)}")
            result.assertNormalExitValue().rethrowFailure()
        }
        Json.parseToJsonElement(stdOut.toString(Charsets.UTF_8))
    }
}
