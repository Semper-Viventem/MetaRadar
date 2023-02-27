@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}
apply {
    plugin("kotlinx-serialization")
}

android {
    compileSdkVersion(33)
    namespace = "f.cking.software"

    defaultConfig {
        applicationId = "f.cking.software"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "0.1-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    val DEBUG = "debug"
    val RELEASE = "release"

    signingConfigs {
        maybeCreate(DEBUG).apply {
            storeFile = file("../signing/debug-keystore.jks")
            storePassword = "metaradar-debug-keystore"
            keyAlias = "meta-radar"
            keyPassword = "metaradar-debug-keystore"
        }
        maybeCreate(RELEASE).apply {
            storeFile = file(gradleLocalProperties(rootDir).getProperty("releaseStoreFile", System.getenv("RELEASE_STORE_PATH") ?: "/"))
            storePassword = gradleLocalProperties(rootDir).getProperty("releaseStorePassword", System.getenv("RELEASE_STORE_PASSWORD") ?: "")
            keyAlias = gradleLocalProperties(rootDir).getProperty("releaseKeyAlias", System.getenv("RELEASE_STORE_KEY") ?: "")
            keyPassword = gradleLocalProperties(rootDir).getProperty("releaseKeyPassword", System.getenv("RELEASE_STORE_KEY_PASSWORD") ?: "")
        }
    }

    buildTypes {
        maybeCreate(DEBUG).apply {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            signingConfig = signingConfigs[DEBUG]
        }
        maybeCreate(RELEASE).apply {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs[RELEASE]
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures.apply {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {

    // kotlin
    implementation(libs.ktx)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.annotation.processing)
    implementation(libs.kotlin.serialization.json)

    // android general
    implementation(libs.appcompat)
    implementation(libs.work.ktx)
    implementation(libs.concurrent.futures)
    implementation(libs.concurrent.futures.ktx)

    // di
    implementation(libs.koin)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compat)
    implementation(libs.koin.android.compose)

    // android jetpack
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel.ktx)

    // compose
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.tooling)
    implementation(libs.lifecycle.compose)
    implementation(libs.compose.activity)
    implementation(libs.compose.dialogs)
    implementation(libs.compose.dialogs.datetime)
    implementation(libs.compose.flow.row)
    debugImplementation(libs.compose.tooling)
    implementation(libs.compose.tooling.preview)

    // room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.ksp)
    kapt(libs.room.ksp)

    // di
    implementation(libs.dagger)

    // Map
    implementation(libs.map)

    implementation(libs.process.phoenix)

    // tests
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
}