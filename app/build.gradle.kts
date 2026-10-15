plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.tgound.cameraxapp"
    compileSdk = 36

    buildFeatures {
        compose = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.tgound.cameraxapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

//    room {
//        schemaDirectory("$projectDir/schemas")
//    }
}

dependencies {
    implementation(project(":shared"))

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.datastore.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    implementation(libs.androidx.viewfinder.view)
    implementation(libs.androidx.viewfinder.core)
    implementation(libs.androidx.viewfinder.compose)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.adaptive)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    implementation(libs.androidx.camera.compose)
    implementation(libs.accompanist.permissions)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Gson
    implementation(libs.gson)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // GIF Encoder - Using alternative library
    // Note: Original library com.github.nbadal:android-gif-encoder is no longer available
    // Temporarily commented out - need to find alternative or implement custom encoder
    // implementation("com.github.nbadal:android-gif-encoder:1.2")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.4.0")
}
