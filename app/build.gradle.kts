plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hisaabwise"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hw.hisaabwise"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Same legal links as the iOS Info.plist. The Terms page is still a placeholder.
        buildConfigField("String", "TERMS_URL", "\"https://hisaabwise.invalid/terms\"")
        buildConfigField("String", "PRIVACY_URL", "\"https://makinananya-cyber.github.io/hisaabwise-legal/privacy.html\"")
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the host machine's localhost as seen from the Android emulator,
            // where the backend runs with `npm run dev` on port 8080.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
            applicationIdSuffix = ".debug"
        }
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "API_BASE_URL", "\"https://hisaabwise-backend-0l9b.onrender.com\"")
            buildConfigField("String", "TERMS_URL", "\"https://staging.hisaabwise.invalid/terms\"")
            applicationIdSuffix = ".staging"
            matchingFallbacks += listOf("debug")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://hisaabwise-backend-0l9b.onrender.com\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // The six languages the iOS app ships.
        localeFilters += listOf("en", "ar", "hi", "fil", "ne", "ur")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
