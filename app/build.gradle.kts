plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdkVersion(33)
    namespace = "f.cking.software"

    defaultConfig {
        applicationId = "f.cking.software"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

dependencies {

    // kotlin
    implementation(libs.ktx)

    // android general
    implementation(libs.appcompat)

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

    // di
    implementation(libs.dagger)

    // tests
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
}