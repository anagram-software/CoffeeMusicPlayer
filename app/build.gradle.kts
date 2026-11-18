import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// API keys are read from local.properties (not committed). Missing keys build as empty strings.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String = "\"${localProperties.getProperty(name, "")}\""

android {
    namespace = "com.udeshcoffee.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.udeshcoffee.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 35
        versionName = "2.0.13"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "GENIUS_ACCESS_TOKEN", secret("genius.accessToken"))
        buildConfigField("String", "LASTFM_API_KEY", secret("lastfm.apiKey"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        // Existing issues as of the Phase 0 migration; later phases should shrink this file.
        baseline = file("lint-baseline.xml")
    }
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DI
    implementation(libs.koin.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.jaudiotagger)

    // Other
    implementation(libs.slidinguppanel)

    testImplementation(libs.junit)
}
