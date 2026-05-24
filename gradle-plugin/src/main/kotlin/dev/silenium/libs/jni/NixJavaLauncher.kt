package dev.silenium.libs.jni

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.file.FileFactory
import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.tasks.Internal
import org.gradle.internal.jvm.inspection.JvmMetadataDetector
import org.gradle.jvm.toolchain.JavaInstallationMetadata
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.internal.DefaultToolchainSpec
import org.gradle.jvm.toolchain.internal.InstallationLocation
import org.gradle.jvm.toolchain.internal.JavaToolchain
import org.gradle.jvm.toolchain.internal.JavaToolchainInput
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.internal.ClientExecHandleBuilderFactory
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.io.path.Path

class NixJavaLauncher @Inject constructor(
    private val flakeDir: Directory,
    private val devShell: String? = null,
    @Inject
    private val toolchainDetector: JvmMetadataDetector,
    @Inject
    private val propertyFactory: PropertyFactory,
    @Inject
    private val fileFactory: FileFactory,
    @Inject
    private val execHandleFactory: ClientExecHandleBuilderFactory,
) : JavaLauncher {
    private val toolchain: JavaToolchain

    init {
        val execBuilder = execHandleFactory.newExecHandleBuilder()
        val shellTarget = buildString {
            append(flakeDir.asFile.absolutePath)
            devShell?.let { append("#$it") }
        }
        execBuilder.commandLine(
            "nix",
            "develop",
            shellTarget,
            "--command",
            "sh", "-c", "echo \$JAVA_HOME",
        )
        execBuilder.workingDir = flakeDir.asFile
        val outputStream = ByteArrayOutputStream()
        execBuilder.standardOutput = outputStream
        execBuilder.errorOutput = ByteArrayOutputStream()
        val execHandle = execBuilder.build()
        execHandle.start().waitForFinish().rethrowFailure()

        val jdkHome = Path(outputStream.toString().trim())
        val metadata = toolchainDetector.getMetadata(InstallationLocation.userDefined(jdkHome.toFile(), "nix"))
        val spec = DefaultToolchainSpec(propertyFactory)
        spec.languageVersion.set(JavaLanguageVersion.of(metadata.languageVersion.majorVersion))
        val input = JavaToolchainInput(spec)
        toolchain = JavaToolchain(metadata, fileFactory, input, false)
    }

    override fun getMetadata(): JavaInstallationMetadata = toolchain

    @Internal
    override fun getExecutablePath(): RegularFile = toolchain.findExecutable("java")
}

fun Project.nixJavaLauncher(flakeDir: Directory = layout.projectDirectory, shellName: String? = null): NixJavaLauncher {
    val toolchainDetector = serviceOf<JvmMetadataDetector>()
    val propertyFactory = serviceOf<PropertyFactory>()
    val fileFactory = serviceOf<FileFactory>()
    val execHandleFactory = serviceOf<ClientExecHandleBuilderFactory>()
    return NixJavaLauncher(flakeDir, shellName, toolchainDetector, propertyFactory, fileFactory, execHandleFactory)
}
