import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Keystore-Konfiguration aus local.properties (nie im Quellcode!)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val ksPath: String = localProps.getProperty("keystore.path", "")
val ksStorePassword: String = localProps.getProperty("keystore.password", "")
val ksKeyAlias: String = localProps.getProperty("key.alias", "")
val ksKeyPassword: String = localProps.getProperty("key.password", "")

android {
    namespace = "com.fantasyfoodplanner"
    compileSdk = 34
    // ndkVersion = "26.1.10909125"  // Deaktiviert - Native C++ Build ausgeblendet
    defaultConfig {
        applicationId = "com.fantasyfoodplanner"
        minSdk = 26
        targetSdk = 34
        versionCode = 19
        versionName = "3.3.0"
        vectorDrawables { useSupportLibrary = true }

        // AI-Plugin: Bei Bedarf ai.api.key in local.properties aktivieren

        // ndk {
        //     abiFilters += listOf("arm64-v8a")
        // }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(ksPath)
            storePassword = ksStorePassword
            keyAlias = ksKeyAlias
            keyPassword = ksKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}
kotlin { jvmToolchain(17) }
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(bom); androidTestImplementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation:1.7.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Lottie fuer lokale JSON-Animationen (View)
    implementation("com.airbnb.android:lottie:6.4.0")
    // Lottie fuer lokale JSON-Animationen (Compose)
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // SceneView fuer 3D Body Selector
    implementation("io.github.sceneview:sceneview:0.10.0")

    // Google ML Kit Text Recognition
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // Google ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Needed for Task.await()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // CameraX für Live-Scanner
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // Retrofit + OkHttp für Open Food Facts API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Unit Tests
    testImplementation("junit:junit:4.13.2")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.0")

    // Firebase Crashlytics
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Encrypted SharedPreferences (API-Key Sicherheit)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Drive Cloud Backup (PRO-Feature)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.http-client:google-http-client-gson:1.44.1") {
        exclude(group = "org.apache.httpcomponents")
    }
}
