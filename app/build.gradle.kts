import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
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
        // Raised to the latest SDK in Phase 1, together with the required behaviour changes.
        targetSdk = 29
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
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.media)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.viewpager)
    implementation(libs.material)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava2)
    ksp(libs.androidx.room.compiler)

    // DI
    implementation(libs.koin.android)

    // Rx
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)
    implementation(libs.sqlbrite)
    implementation(libs.rxbroadcast)
    implementation(libs.rxbinding.appcompat)
    implementation(libs.annimon.stream)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter.rxjava2)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.jsoup)

    // Media
    implementation(libs.glide)

    // Other
    implementation(libs.permiso)
    implementation(libs.slidinguppanel)

    testImplementation(libs.junit)
}
