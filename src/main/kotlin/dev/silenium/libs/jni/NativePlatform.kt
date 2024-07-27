package dev.silenium.libs.jni

import org.apache.commons.lang3.ArchUtils
import java.io.ObjectOutputStream
import java.io.Serializable

data class Platform(val os: OS, val arch: Arch, val extension: String? = null) : Serializable {
    constructor(platformString: String, extension: String? = null) : this(
        OS.values().first { it.name.equals(platformString.split("-").first(), ignoreCase = true) },
        Arch.values().first { it.name.equals(platformString.split("-").getOrNull(1), ignoreCase = true) },
        extension,
    )

    enum class OS {
        WINDOWS, LINUX, DARWIN;

        override fun toString() = name.lowercase()
    }

    enum class Arch {
        X86, X86_64, ARM, ARM64;

        override fun toString() = name.lowercase()
    }

    @Transient
    val full = "$os-$arch${extension ?: ""}"

    @Transient
    val osArch = "$os-$arch"

    @Transient
    val capitalized = full
        .split("-")
        .joinToString("") {
            it.replaceFirstChar(Char::uppercaseChar)
                .replace(Regex("[^a-zA-Z0-9_]+"), "_")
        }

    override fun toString() = full

    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
    }
}

interface INativePlatform {
    val os: Platform.OS
    val arch: Platform.Arch
    fun platform(extension: String? = null): Platform
}

class NativePlatform(osString: String, archString: String) : INativePlatform {
    private val osString = osString.lowercase()
    private val archString = archString.lowercase()

    override val os = when {
        isDarwin() -> Platform.OS.DARWIN
        isLinux() -> Platform.OS.LINUX
        isWindows() -> Platform.OS.WINDOWS
        else -> throw UnsupportedOperationException("Unsupported OS: $osString")
    }

    override val arch = when {
        isX8664() -> Platform.Arch.X86_64
        isX86() -> Platform.Arch.X86
        isArm64() -> Platform.Arch.ARM64
        isArm() -> Platform.Arch.ARM

        else -> throw UnsupportedOperationException("Unsupported architecture: $archString")
    }

    override fun platform(extension: String?): Platform {
        return Platform(os, arch, extension)
    }

    private fun isWindows(): Boolean = osString.contains("win")
    private fun isLinux(): Boolean = osString.contains("nix") || osString.contains("nux")

    private fun isDarwin(): Boolean =
        osString.contains("mac") || osString.contains("darwin") || osString.contains("osx") || osString.contains("ios")

    private fun isArm(): Boolean = archString.contains("arm")
    private fun isArm64(): Boolean = ArchUtils.getProcessor(archString)?.isAarch64 == true || archString == "arm64"

    private fun isX86(): Boolean = ArchUtils.getProcessor(archString)?.let {
        it.isX86 && it.is32Bit
    } == true

    private fun isX8664(): Boolean = ArchUtils.getProcessor(archString)?.let {
        it.isX86 && it.is64Bit
    } == true

    companion object : INativePlatform by NativePlatform(System.getProperty("os.name"), System.getProperty("os.arch"))
}
