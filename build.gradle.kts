plugins {
    alias(libs.plugins.conventions.jvm)
    alias(libs.plugins.conventions.plugin) apply false
}

allprojects {
    group = "dev.silenium.libs.jni"
}
