@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wireguard.android.tunnel"
    compileSdk = 34
    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 26
    }

    externalNativeBuild {
        cmake { path = file("tools/CMakeLists.txt") }
    }

    buildTypes {
        all {
            externalNativeBuild {
                cmake {
                    targets("libwg-go.so", "libwg.so", "libwg-quick.so")
                    arguments("-DGRADLE_USER_HOME=${project.gradle.gradleUserHomeDir}")
                }
            }
        }
        debug {
            externalNativeBuild {
                cmake { arguments("-DANDROID_PACKAGE_NAME=com.cloudflaresecure.vpn.debug") }
            }
        }
        release {
            externalNativeBuild {
                cmake { arguments("-DANDROID_PACKAGE_NAME=com.cloudflaresecure.vpn") }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection)
    compileOnly(libs.jsr305)
}
