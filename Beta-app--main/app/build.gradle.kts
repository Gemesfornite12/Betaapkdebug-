plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: "MY_GEMINI_API_KEY"}\"")
            buildConfigField("String", "OPENROUTESERVICE_API_KEY", "\"${System.getenv("OPENROUTESERVICE_API_KEY") ?: "MY_OPENROUTESERVICE_API_KEY"}\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"${System.getenv("GIPHY_API_KEY") ?: "Sp8FgZ0UZm703LiBx3RutbdHH3q7XODd"}\"")
            buildConfigField("String", "KLIPY_API_KEY", "\"${System.getenv("KLIPY_API_KEY") ?: "vphHNolsolz"}\"")
            buildConfigField("String", "SUPABASE_PROJECT_URL", "\"${System.getenv("SUPABASE_PROJECT_URL") ?: "https://ovttmxwtljfqizcetoxk.supabase.co"}\"")
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: "sb_publishable_MXe6r7RgToi2YjygMSIm4A_dLjM1dwG"}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: "MY_GEMINI_API_KEY"}\"")
            buildConfigField("String", "OPENROUTESERVICE_API_KEY", "\"${System.getenv("OPENROUTESERVICE_API_KEY") ?: "MY_OPENROUTESERVICE_API_KEY"}\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"${System.getenv("GIPHY_API_KEY") ?: "Sp8FgZ0UZm703LiBx3RutbdHH3q7XODd"}\"")
            buildConfigField("String", "KLIPY_API_KEY", "\"${System.getenv("KLIPY_API_KEY") ?: "vphHNolsolz"}\"")
            buildConfigField("String", "SUPABASE_PROJECT_URL", "\"${System.getenv("SUPABASE_PROJECT_URL") ?: "https://ovttmxwtljfqizcetoxk.supabase.co"}\"")
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: "sb_publishable_MXe6r7RgToi2YjygMSIm4A_dLjM1dwG"}\"")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.base)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.maplibre.android)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.language.id)
    implementation(libs.mlkit.translate)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
