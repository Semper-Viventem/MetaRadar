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
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        maybeCreate("debug").apply {
            isMinifyEnabled = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.ksp)
    kapt(libs.room.ksp)

    // di
    implementation(libs.dagger)

    // Map
    implementation(libs.map)

    // tests
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
}