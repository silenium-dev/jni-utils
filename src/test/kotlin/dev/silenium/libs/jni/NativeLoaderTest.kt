package dev.silenium.libs.jni

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NativeLoaderTest : StringSpec({
    "loads library" {
        NativeLoader.loadLibraryFromClasspath("test")
        stringFromJNI() shouldBe "jni-string"
    }

    "resolves correct path" {
        NativeLoader.libPath("test", basePath = "base", platform = Platform(Platform.OS.WINDOWS, Platform.Arch.ARM64)) shouldBe "base/windows-arm64/test.dll"
        NativeLoader.libPath("test", basePath = "base", platform = Platform(Platform.OS.WINDOWS, Platform.Arch.X86)) shouldBe "base/windows-x86/test.dll"
        NativeLoader.libPath("test", basePath = "base", platform = Platform(Platform.OS.DARWIN, Platform.Arch.ARM)) shouldBe "base/darwin-arm/libtest.dylib"
    }
})
