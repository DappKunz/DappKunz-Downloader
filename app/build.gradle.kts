import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Kredensial signing dimuat dari environment (CI) atau dari app/keystore.properties (lokal).
// File keystore.properties dan *.jks tidak pernah masuk repo (lihat .gitignore).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStorePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
val releaseKeyPassword = signingValue("KEY_PASSWORD", "keyPassword")
val releaseKeyAlias = signingValue("KEY_ALIAS", "keyAlias")
val releaseStoreFilePath = signingValue("KEYSTORE_PATH", "storeFile")
val releaseStoreFile = releaseStoreFilePath?.let {
    val candidate = file(it)
    if (candidate.exists()) candidate else rootProject.file(it)
}
val hasReleaseSigning =
    releaseStoreFile != null && releaseStorePassword != null && releaseKeyPassword != null && releaseKeyAlias != null

android {
    namespace = "com.downloader.app"
    compileSdk = 35

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.downloader.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 10104
        versionName = "1.1.4_Build-7"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Tanpa kredensial signing, build tetap menghasilkan APK -unsigned (untuk lokal).
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("Release signing tidak dikonfigurasi: APK akan unsigned. Isi app/keystore.properties atau set env KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD.")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.documentfile)

    implementation(files("libs/library-0.18.1-slim.aar"))
    implementation(files("libs/ffmpeg-0.18.1-slim.aar"))
    implementation(libs.youtubedl.common)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
