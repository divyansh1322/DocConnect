plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.docconnect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.docconnect"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- CRITICAL FIX FOR LISTENABLEFUTURE ERROR ---
    // Moving this here ensures it applies to all configurations during the build process
    configurations.all {
        resolutionStrategy {
            // Fresher/Fresco image library forces
            force("com.facebook.fresco:fresco:3.4.0")
            force("com.facebook.fresco:imagepipeline:3.4.0")
            force("com.facebook.fresco:imagepipeline-okhttp3:3.4.0")
            force("com.facebook.fresco:ui-common:3.4.0")

            // THE FIX: Forcing the listener future to prevent 'class file not found' errors.
            // This is the "dummy" version that resolves conflicts between Guava and AndroidX.
            force("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
        }
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
}

dependencies {
    // --- 1. Firebase (BoM Managed) ---
    // Using BoM ensures all Firebase libraries work together without version mismatch
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth")

    // --- 2. App Check (Security) ---
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")

    // --- 3. UI and Layout ---
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- 4. Location and Background Tasks ---
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // --- 5. Image Handling & Effects ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // --- 6. Animations & Utilities ---
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.android.volley:volley:1.2.1")

    // --- 7. Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //-----8.WorkManager -- This library is the one that primarily requires ListenableFuture.
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("com.google.guava:guava:31.1-android")
}