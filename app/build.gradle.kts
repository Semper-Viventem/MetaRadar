import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

room {
    schemaDirectory("$projectDir/schemas")
}

apply {
    plugin("kotlinx-serialization")
}

android {
    compileSdk = 34
    namespace = "f.cking.software"

    defaultConfig {
        applicationId = "f.cking.software"
        minSdk = 29
        targetSdk = 34

        versionCode = 1708536354
        versionName = "0.25.2-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "REPORT_ISSUE_URL", "\"https://github.com/Semper-Viventem/MetaRadar/issues\"")
        buildConfigField("String", "GITHUB_URL", "\"https://github.com/Semper-Viventem/MetaRadar\"")
        buildConfigField("String", "STORE_PAGE_URL", "\"Not specified\"")
        buildConfigField("String", "MAP_LICENSE_URL", "\"https://www.openstreetmap.org/copyright\"")
        buildConfigField("Boolean", "OFFLINE_MODE_DEFAULT_STATE", "false")

        buildConfigField("String", "DISTRIBUTION", "\"Not specified\"")
    }

    val DEBUG = "debug"
    val RELEASE = "release"

    val NO_SIGNING_CONFIG = "no_signing_store"

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

            val hasSignConfig = gradleLocalProperties(rootDir).getProperty("releaseStoreFile", System.getenv("RELEASE_STORE_PATH") ?: NO_SIGNING_CONFIG) != NO_SIGNING_CONFIG

            signingConfig = if (hasSignConfig) signingConfigs[RELEASE] else null
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            isDefault = true
            dimension = "distribution"

            buildConfigField("String", "DISTRIBUTION", "\"Github\"")
            buildConfigField("Boolean", "STORE_RATING_IS_APPLICABLE", "false")
            buildConfigField("String", "STORE_PAGE_URL", "\"https://github.com/Semper-Viventem/MetaRadar/releases?q=release+build\"")
        }
        create("googlePlay") {
            isDefault = false
            dimension = "distribution"

            buildConfigField("String", "DISTRIBUTION", "\"Google play\"")
            buildConfigField("Boolean", "STORE_RATING_IS_APPLICABLE", "true")
            buildConfigField("String", "STORE_PAGE_URL", "\"https://play.google.com/store/apps/details?id=f.cking.software&pcampaignid=web_share\"")
        }
        create("fdroid") {
            isDefault = false
            dimension = "distribution"

            buildConfigField("Boolean", "OFFLINE_MODE_DEFAULT_STATE", "true")
            buildConfigField("Boolean", "STORE_RATING_IS_APPLICABLE", "false")
            buildConfigField("String", "DISTRIBUTION", "\"F-Droid\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures.apply {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
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
    implementation(libs.compose.tooling)
    implementation(libs.lifecycle.compose)
    implementation(libs.compose.activity)
    implementation(libs.compose.dialogs)
    implementation(libs.compose.dialogs.datetime)
    implementation(libs.compose.flow.row)
    implementation(libs.ktx)
    debugImplementation(libs.compose.tooling)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)

    // room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.ksp)
    ksp(libs.room.ksp)

    // di
    implementation(libs.dagger)

    // Map
    implementation(libs.map)

    // app restart
    implementation(libs.process.phoenix)

    // logger
    implementation(libs.timber)

    // tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ktx.testing)
}