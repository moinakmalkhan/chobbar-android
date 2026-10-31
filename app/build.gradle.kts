import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Optional local overrides (not committed): API_URL, WS_URL, GOOGLE_WEB_CLIENT_ID, release signing.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String, default: String) = (localProps.getProperty(name) ?: System.getenv(name) ?: default)

android {
    namespace = "shop.chobbar.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "shop.chobbar.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        // Ship only the languages we support (keeps the APK small).
        resourceConfigurations += listOf("en", "ur", "ar")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${prop("GOOGLE_WEB_CLIENT_ID", "")}\"")
    }

    signingConfigs {
        create("release") {
            val store = prop("RELEASE_STORE_FILE", "")
            if (store.isNotEmpty()) {
                storeFile = file(store)
                storePassword = prop("RELEASE_STORE_PASSWORD", "")
                keyAlias = prop("RELEASE_KEY_ALIAS", "")
                keyPassword = prop("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // 10.0.2.2 is the host machine from the Android emulator.
            buildConfigField("String", "API_URL", "\"${prop("API_URL", "http://10.0.2.2:8000/api/v1/")}\"")
            buildConfigField("String", "WS_URL", "\"${prop("WS_URL", "ws://10.0.2.2:8000/ws/")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_URL", "\"${prop("API_URL", "https://api.chobbar.shop/api/v1/")}\"")
            buildConfigField("String", "WS_URL", "\"${prop("WS_URL", "wss://api.chobbar.shop/ws/")}\"")
            if (prop("RELEASE_STORE_FILE", "").isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.coil.compose)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
}
