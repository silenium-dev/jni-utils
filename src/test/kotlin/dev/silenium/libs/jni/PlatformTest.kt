package dev.silenium.libs.jni

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class PlatformTest : StringSpec({
    "formatting is correct" {
        val platform = Platform(Platform.OS.LINUX, Platform.Arch.X86, ".so")
        platform.extension shouldBe ".so"
        platform.full shouldBe "linux-x86.so"
    }

    "capitalization is correct" {
        val platform = Platform(Platform.OS.LINUX, Platform.Arch.X86, "-gpl")
        platform.capitalized shouldBe "LinuxX86Gpl"
    }

    "serialization works" {
        val platform = Platform(Platform.OS.LINUX, Platform.Arch.X86, ".so")
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(platform)
        val bais = baos.toByteArray().inputStream()
        val ois = ObjectInputStream(bais)
        val deserialized = ois.readObject() as Platform
        deserialized shouldBe platform
    }
})
