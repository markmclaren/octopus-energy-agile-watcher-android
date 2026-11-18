import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// Load Octopus credentials from .env and/or local.properties (gitignored).
// Copy local.properties.example -> local.properties and fill in your values,
// or enter your API key directly in the app's Onboarding / Settings screen.
// ---------------------------------------------------------------------------
val localProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) load(envFile.inputStream())
    val propFile = rootProject.file("local.properties")
    if (propFile.exists()) load(propFile.inputStream())
}

fun localProp(key: String, fallback: String = ""): String {
    val raw = localProps.getProperty(key, System.getenv(key) ?: fallback)
    return "\"${raw.replace("\"", "\\\"")}\""
}

android {
    namespace = "com.example.agilewatcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.agilewatcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Inject optional build-time defaults — never hardcoded in source.
        buildConfigField("String", "OCTOPUS_API_KEY",          localProp("OCTOPUS_API_KEY"))
        buildConfigField("String", "OCTOPUS_ACCOUNT_NUMBER",   localProp("OCTOPUS_ACCOUNT_NUMBER"))
        buildConfigField("String", "OCTOPUS_BASE_URL",         localProp("OCTOPUS_BASE_URL", "https://api.oegb-kraken.energy"))
        buildConfigField("String", "OCTOPUS_DEFAULT_PRODUCT",  localProp("OCTOPUS_DEFAULT_PRODUCT", "AGILE-24-10-01"))
        buildConfigField("String", "OCTOPUS_DEFAULT_REGION",   localProp("OCTOPUS_DEFAULT_REGION", "C"))
        buildConfigField("String", "OCTOPUS_ELECTRIC_MPAN",    localProp("OCTOPUS_ELECTRIC_MPAN"))
        buildConfigField("String", "OCTOPUS_ELECTRIC_SERIAL",  localProp("OCTOPUS_ELECTRIC_SERIAL"))
        buildConfigField("String", "OCTOPUS_GAS_MPRN",         localProp("OCTOPUS_GAS_MPRN"))
        buildConfigField("String", "OCTOPUS_GAS_SERIAL",       localProp("OCTOPUS_GAS_SERIAL"))
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = localProps.getProperty("KEYSTORE_STORE_PASSWORD", "agilewatcher123")
                keyAlias = localProps.getProperty("KEYSTORE_KEY_ALIAS", "agilewatcher")
                keyPassword = localProps.getProperty("KEYSTORE_KEY_PASSWORD", "agilewatcher123")
                enableV1Signing = true
                enableV2Signing = true
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Compose Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Jetpack Compose BOM & UI
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Networking & JSON / Coroutines
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
