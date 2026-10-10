import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.antigravity.adaptivehz"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antigravity.adaptivehz"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val localProperties = Properties()
            val propFile = rootProject.file("local.properties")
            if (propFile.exists()) {
                localProperties.load(propFile.inputStream())
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            val releaseKeystore = rootProject.file("adaptivehz-release.keystore")
            if (releaseKeystore.exists()) {
                storeFile = releaseKeystore
                storePassword = localProperties.getProperty("keystore.storePassword") ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = localProperties.getProperty("keystore.keyAlias") ?: System.getenv("KEYSTORE_ALIAS") ?: "adaptivehz"
                keyPassword = localProperties.getProperty("keystore.keyPassword") ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            } else {
                val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = localProperties.getProperty("keystore.storePassword") ?: "android"
                    keyAlias = localProperties.getProperty("keystore.keyAlias") ?: "androiddebugkey"
                    keyPassword = localProperties.getProperty("keystore.keyPassword") ?: "android"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
}
