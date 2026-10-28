plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.acuranza.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sumvalo.app"
        // English-only UI (standing decision): library locale tables for the other ~80
        // languages never ship, which trims resources.arsc.
        resourceConfigurations += setOf("en")
        // Universal native libraries: the app runs on phones and emulators alike.
        // arm64-v8a (64-bit ARM), armeabi-v7a (32-bit ARM), x86 (32-bit Intel/AMD),
        // x86_64 (64-bit Intel/AMD) are all packaged so any compatible device can install.
        // SQLCipher native libs ship for every one of these ABIs; APK size grows accordingly.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.8"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // The release key is generated outside source control; see docs/BUILDING.md. Only the path is fixed here.
            val ks = rootProject.file("keystore/sumvalo-release.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = providers.gradleProperty("SUMVALO_STORE_PASSWORD").orNull
                keyAlias = "sumvalo"
                keyPassword = providers.gradleProperty("SUMVALO_KEY_PASSWORD").orNull
            }
        }
    }

    // Sumvalo is offline-only by design: the app has no INTERNET permission, so Android itself
    // guarantees that nothing can leave the phone. There is a single edition.

    // Two capability variants of the same app. "standard" is the sideloaded release: no SMS
    // permission, no notification listener — Play Protect installs it like any app. "full"
    // restores automatic SMS capture (historical inbox import + notification tracking) for
    // Play-distribution with the declared SMS-based money-management exception; sideloading
    // the full APK will very likely trip Play Protect's install-time warning — that is
    // understood and accepted by the user (choice made 2026-10-20).
    flavorDimensions += "capabilities"
    productFlavors {
        create("standard") {
            buildConfigField("boolean", "SMS_FULL", "false")
        }
        create("full") {
            buildConfigField("boolean", "SMS_FULL", "true")
            versionNameSuffix = "-full"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the Sumvalo key when it is present locally. F-Droid builds without it
            // and signs with its own key. Note: the build is NOT byte-reproducible (AGP embeds
            // build timestamps), so hash equality across machines cannot be used for verification;
            // use the recorded SHA-256 of each published release artifact instead.
            if (rootProject.file("keystore/sumvalo-release.jks").exists()) signingConfig = signingConfigs.getByName("release")
        }
        // Debug builds use the standard debug key, so a debuggable APK can never install over the real app.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Native libraries are packaged exactly as the libraries ship them. Stripping depends on which NDK the build
        // machine has, which would make F-Droid's rebuild differ from the release APK (reproducible builds).
        jniLibs.keepDebugSymbols += "**/*.so"
    }
    // No "dependency metadata" in the APK signing block: it is encrypted for Google Play only, and F-Droid's scanner
    // rejects APKs that carry it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:sms"))
    implementation(project(":core:finance"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
