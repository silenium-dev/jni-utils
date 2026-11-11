package dev.silenium.libs.jni

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance

interface NixEvalSpec {
    val workingDir: DirectoryProperty
    val expression: Property<String>
}

fun Project.nixEval(config: Action<NixEvalSpec>): Provider<JsonElement> {
    val spec = objects.newInstance<NixEvalSpec>()
    config.execute(spec)
    val cmdLine = listOf("nix", "eval", "--json", spec.expression)
    val execResult = project.providers.exec {
        it.commandLine(cmdLine)
        it.workingDir(spec.workingDir)
        it.isIgnoreExitValue = true
    }
    return execResult.result.map { result ->
        if (result.exitValue != 0) {
            val stderr = execResult.standardError.asText.get()
            throw GradleException("nix eval failed with exit code ${result.exitValue}:\n$stderr")
        }
        Json.parseToJsonElement(execResult.standardOutput.asText.get())
    }
}
