import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.room")
    alias(libs.plugins.compose.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

apply {
    plugin("kotlinx-serialization")
}

android {
    compileSdk = 36
    namespace = "f.cking.software"
    val javaConfig: JavaConfig = JavaConfig.getByString(getEnvJavaConfigVersion())

    defaultConfig {
        applicationId = "f.cking.software"
        minSdk = 29
        targetSdk = 36

        versionCode = 1708536379
        versionName = "0.31.2-beta"

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
            storeFile = file(gradleLocalProperties(rootDir, providers).getProperty("releaseStoreFile", System.getenv("RELEASE_STORE_PATH") ?: "/"))
            storePassword = gradleLocalProperties(rootDir, providers).getProperty("releaseStorePassword", System.getenv("RELEASE_STORE_PASSWORD") ?: "")
            keyAlias = gradleLocalProperties(rootDir, providers).getProperty("releaseKeyAlias", System.getenv("RELEASE_STORE_KEY") ?: "")
            keyPassword = gradleLocalProperties(rootDir, providers).getProperty("releaseKeyPassword", System.getenv("RELEASE_STORE_KEY_PASSWORD") ?: "")
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

            val hasSignConfig = gradleLocalProperties(rootDir, providers).getProperty("releaseStoreFile", System.getenv("RELEASE_STORE_PATH") ?: NO_SIGNING_CONFIG) != NO_SIGNING_CONFIG

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
        sourceCompatibility = javaConfig.javaVersion
        targetCompatibility = javaConfig.javaVersion
    }

    kotlin {
        jvmToolchain(javaConfig.jdkVersion)
    }

    kotlinOptions {
        jvmTarget = javaConfig.jvmTarget
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
    implementation(libs.compose.material.icons)

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
    testImplementation(libs.mockk)
    testImplementation(libs.ktx.testing)
    testImplementation(libs.ktx.testing.core)
    androidTestImplementation(libs.ktx.testing)
}

private fun getEnvJavaConfigVersion(): String {
    val version = gradleLocalProperties(rootDir, providers).getProperty("JAVA_CONFIG_VERSION", System.getenv("JAVA_CONFIG_VERSION") ?: "UNSPECIFIED")
    println("Environment JDK version selected is ${version}. To override it define JAVA_CONFIG_VERSION environment variable or local properties.")
    return version
}

enum class JavaConfig(val jvmTarget: String, val jdkVersion: Int, val javaVersion: JavaVersion) {
    JAVA_21("21", 21, JavaVersion.VERSION_21),
    JAVA_22("22", 22, JavaVersion.VERSION_22);

    companion object {
        fun getByString(versionStr: String?): JavaConfig {
            return when (versionStr) {
                "21" -> JAVA_21
                "22" -> JAVA_22
                else -> {
                    println("Java version ${versionStr} is not recognized. The default one will be used instead for this project: ${DEFAULT.jvmTarget}")
                    DEFAULT
                }
            }
        }

        private val DEFAULT = JAVA_21
    }
}