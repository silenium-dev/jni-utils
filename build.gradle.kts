import dev.silenium.gradle.conventions.publishing

plugins {
    alias(libs.plugins.conventions.jvm)
    alias(libs.plugins.conventions.plugin) apply false
}

allprojects {
    group = "dev.silenium.libs.jni"

    afterEvaluate {
        conventions {
            publishing {
                pomSpec.set {
                    name = project.name
                    description = "A library for working with native libraries"
                    url = "https://github.com/silenium-dev/jni-utils"
                    inceptionYear = "2024"
                    licenses {
                        license {
                            name = "GPL-3.0-or-later"
                            url = "https://spdx.org/licenses/GPL-3.0-or-later.html"
                        }
                    }
                    developers {
                        developer {
                            id = "silenium-dev"
                            email = "support@silenium-dev.net"
                        }
                    }
                    scm {
                        connection = "scm:git:git://github.com/silenium-dev/jni-utils.git"
                        developerConnection = "scm:git:ssh://github.com/silenium-dev/jni-utils.git"
                        url = "https://github.com/silenium-dev/jni-utils"
                    }
                }
            }
        }
    }
}
