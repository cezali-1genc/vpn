plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

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
            // İmzalama: keystore.properties + ortam değişkenlerinden okunur.
            // Anahtar dosyası repoya ASLA eklenmez (spec §13).
            val keystoreProps = java.util.Properties().apply {
                val f = rootProject.file("keystore.properties")
                if (f.exists()) load(java.io.FileInputStream(f))
            }
            val ksPath = keystoreProps.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_PATH")
            if (ksPath != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(ksPath)
                    storePassword = keystoreProps.getProperty("storePassword")
                        ?: System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                        ?: System.getenv("KEY_ALIAS")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                        ?: System.getenv("KEY_PASSWORD")
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

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
    implementation(libs.wireguard.tunnel)
    implementation(libs.wireguard.config)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
