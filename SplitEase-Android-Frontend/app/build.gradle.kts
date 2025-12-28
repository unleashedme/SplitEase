plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.splitease"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.splitease"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // --- Room (Local DB) ---
    implementation ("androidx.room:room-runtime:2.8.4")
    implementation(libs.androidx.constraintlayout.core)
    kapt ("androidx.room:room-compiler:2.8.4")
    implementation ("androidx.room:room-ktx:2.8.4")
    implementation ("androidx.room:room-paging:2.8.4")
    // -------------------- Navigation (Compose) --------------------
    implementation("androidx.navigation:navigation-compose:2.8.7")
    // -------------------- Lifecycle / ViewModel --------------------
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    // -------------------- Hilt (Dependency Injection) --------------------
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation ("androidx.hilt:hilt-navigation-compose:1.0.0")
    // -------------------- Retrofit + OkHttp + Moshi --------------------
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("com.squareup.moshi:moshi-kotlin:1.15.0")
    kapt ("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    // -------------------- WorkManager (Background sync & reminders) --------------------
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    // -------------------- DataStore (Preferences) --------------------
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    // -------------------- Image Loading --------------------
    implementation("io.coil-kt:coil-compose:2.4.0")
    // -------------------- Charts / Analytics --------------------
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")

    // firebase messaging dependency
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.0")
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    // Import the Firebase BoM
    implementation("com.google.firebase:firebase-analytics")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}