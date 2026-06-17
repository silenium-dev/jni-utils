package dev.silenium.libs.jni

import org.jetbrains.annotations.Blocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

interface INativeLoader {
    val nativePlatform: INativePlatform
    fun fileNameTemplate(platform: Platform = nativePlatform.platform()): String

    @Blocking
    fun loadLibraryFromClasspath(
        baseName: String,
        basePath: String? = DEFAULT_BASE_PATH,
        platform: Platform = NativePlatform.platform(),
    ): Result<Path>

    @Blocking
    fun extractFileFromClasspath(resourcePath: String): Result<Path>
    fun libPath(
        baseName: String,
        basePath: String? = DEFAULT_BASE_PATH,
        platform: Platform = nativePlatform.platform(),
    ): String

    companion object {
        const val DEFAULT_BASE_PATH: String = "natives"
    }
}

class NativeLoader(override val nativePlatform: INativePlatform) : INativeLoader {
    private val dir = Files.createTempDirectory("jni-utils-natives")
    private val libs = mutableMapOf<String, Path>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            dir.toFile().deleteRecursively()
        })
    }

    override fun fileNameTemplate(platform: Platform) = when (platform.os) {
        Platform.OS.WINDOWS -> "%s.dll"
        Platform.OS.LINUX -> "lib%s.so"
        Platform.OS.DARWIN -> "lib%s.dylib"
    }

    @Blocking
    override fun loadLibraryFromClasspath(
        baseName: String,
        basePath: String?,
        platform: Platform,
    ): Result<Path> = runCatching {
        require(!baseName.contains("/")) { "baseName cannot contain '/'" }

        val libResourcePath = libPath(baseName, basePath, platform)
        val libFile = libs.getOrPut(libResourcePath) {
            extractFileFromClasspath(libResourcePath).getOrThrow()
        }
        System.load(libFile.absolutePathString())
        libFile
    }

    @Blocking
    override fun extractFileFromClasspath(resourcePath: String): Result<Path> = runCatching {
        val classLoader = Thread.currentThread().contextClassLoader
        val outputFile = dir.resolve(resourcePath)
        outputFile.parent.createDirectories()
        classLoader.getResourceAsStream(resourcePath)?.use { input ->
            outputFile.outputStream().use(input::copyTo)
        } ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        outputFile
    }

    override fun libPath(
        baseName: String,
        basePath: String?,
        platform: Platform,
    ): String {
        val sanitizedBasePath = basePath?.trim('/')?.plus("/") ?: ""
        val fileName = fileNameTemplate(platform).format(baseName)
        return "$sanitizedBasePath$platform/$fileName"
    }

    companion object : INativeLoader by NativeLoader(NativePlatform)
}
