import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.pocketai.app"
    compileSdk = 36
    ndkVersion = "26.1.10909125" // Typically required for NDK builds

    defaultConfig {
        applicationId = "com.pocketai.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.3.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "LOGO_DEV_TOKEN", "\"${localProps.getProperty("LOGO_DEV_TOKEN", "")}\"")
        buildConfigField("String", "FIREBASE_HOSTING_URL", "\"${localProps.getProperty("FIREBASE_HOSTING_URL", "https://pocket-ai-a51d5.web.app")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("STORE_FILE", "../keystore.jks"))
            storePassword = localProps.getProperty("STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("KEY_ALIAS", "")
            keyPassword = localProps.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
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
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }


    defaultConfig {
        // Configure NDK ABI filters (arm64-v8a for modern Android devices)
        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
        externalNativeBuild {
            cmake {
                arguments("-DCMAKE_BUILD_TYPE=Release")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Disable compression for large model files
    androidResources {
        noCompress.addAll(listOf(
            "bin", "task", "tflite", "gguf", "litertlm",
            "part1", "part2", "part3", "part4", "part5", "part6", "part7", "part8", "part9",
            "tdata"
        ))
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/module-info.class"
            pickFirsts += "META-INF/INDEX.LIST"
            pickFirsts += "META-INF/LICENSE"
            pickFirsts += "META-INF/LICENSE.txt"
            pickFirsts += "META-INF/NOTICE"
            pickFirsts += "META-INF/NOTICE.md"
            pickFirsts += "META-INF/io.netty.versions.properties"
            pickFirsts += "META-INF/native/libnetty_transport_native_epoll_x86_64.so"
            pickFirsts += "META-INF/native/libnetty_transport_native_kqueue_x86_64.so"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Networking (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Room database (KAPT)
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Data Store (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Hilt (KAPT)
    implementation("com.google.dagger:hilt-android:2.54")
    kapt("com.google.dagger:hilt-android-compiler:2.54")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // GGUF LLM Inference - RealGGUFInference (simulated)
    // Will be replaced with actual java-llama.cpp when available
    
    // ML Kit Document Scanner (edge detection + auto-crop)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
