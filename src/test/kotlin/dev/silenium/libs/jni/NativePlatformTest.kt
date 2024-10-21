package dev.silenium.libs.jni

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NativePlatformTest : StringSpec({
    beforeEach {
        val os = System.getProperty("os.name")
        val arch = System.getProperty("os.arch")
        afterEach {
            System.setProperty("os.name", os)
            System.setProperty("os.arch", arch)
        }
    }

    data class Parameter(val expectedPlatform: Platform, val input: Set<Pair<String, String>>)

    val parameters = Platform.OS.entries.flatMap { os ->
        Platform.Arch.entries.map { arch ->
            (0 until 10).map {
                val randomPrefix = ('a'..'z').filter { it !in setOf('w', 'm', 'x', 'i') }.shuffled().take(5).joinToString("")
                val randomSuffix = ('a'..'z').filter { it !in setOf('w', 'm', 'x', 'i') }.shuffled().take(5).joinToString("")
                "$randomPrefix$os$randomSuffix" to "$arch"
            }.let {
                Parameter(Platform(os, arch), it.toSet())
            }
        }
    }

    parameters.forEach { parameter ->
        parameter.input.forEach { input ->
            "resolves os=${input.first} and arch=${input.second} to ${parameter.expectedPlatform}" {
                val platform = NativePlatform(input.first, input.second).platform()
                platform shouldBe parameter.expectedPlatform
            }
        }
    }
})
