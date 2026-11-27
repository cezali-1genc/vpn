import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// İmzalama bilgileri android bloğu DIŞINDA okunur (Gradle Kotlin DSL uyumu).
// Anahtar dosyası repoya ASLA eklenmez (spec §13).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(FileInputStream(f))
}
val ksPath: String? = keystoreProps.getProperty("storeFile")
    ?: System.getenv("KEYSTORE_PATH")
val ksStorePassword: String? = keystoreProps.getProperty("storePassword")
    ?: System.getenv("KEYSTORE_PASSWORD")
val ksKeyAlias: String? = keystoreProps.getProperty("keyAlias")
    ?: System.getenv("KEY_ALIAS")
val ksKeyPassword: String? = keystoreProps.getProperty("keyPassword")
    ?: System.getenv("KEY_PASSWORD")

android {
    namespace = "com.cloudflaresecure.vpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cloudflaresecure.vpn"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (ksPath != null) {
                storeFile = File(ksPath)
                storePassword = ksStorePassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    // Resmî WireGuard Android Tunnel Library (spec §2)
    // Not: com.wireguard.config.* sınıfları tunnel artifact'ının içindedir.
    implementation(libs.wireguard.tunnel)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
