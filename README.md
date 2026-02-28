# jni-utils

A Kotlin library for platform-specific loading of jni bindings.

## Usage

You can add the dependency to your project as follows:

```kotlin
repositories {
    maven("https://repoflow.silenium.dev/api/maven/public/maven-releases") {
        name = "silenium-releases"
    }
}
dependencies {
    implementation("dev.silenium.libs.jni:jni-utils:0.2.0")
}
```

### Example

File structure:
```
src
└── main
    ├── kotlin
    │   └── Main.kt
    └── resources
        └── lib
            ├── linux-x86_64
            │   └── libnative.so
            └── windows-x86_64
                └── native.dll
```

`Main.kt`
```kotlin
import dev.silenium.libs.jni.NativeLoader
import java.nio.file.Path

fun main() {
    // Loads from linux-x86_64/libnative.so on Linux x86_64
    // Loads from windows-x86_64/native.dll on Windows x86_64
    val result: Result<Path> = NativeLoader.loadLibraryFromClasspath(baseName = "native", basePath = "lib")
    if (result.isFailure) { // if file not found or failed to load
        println("Failed to load library: ${result.exceptionOrNull()}")
        return
    }
    val path = result.getOrThrow() // path to the extracted library file
    println("Loaded library: $path")
}
```
