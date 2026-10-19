import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

// Secrets live in the root local.properties (gitignored), never in source.
// See local.properties.example for the expected keys.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String = localProperties.getProperty(name, "")

val appVersionName = "1.0"

// Optional, for size-limited debug uploads only: ./gradlew assembleDebug -PdebugAbi=arm64-v8a
// Unset (the default) leaves the ABI config unchanged.
val debugAbi: String? = providers.gradleProperty("debugAbi").orNull?.takeIf { it.isNotBlank() }

android {
    namespace = "com.tgound.photogether"
    compileSdk = 36

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.tgound.photogether"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"${secret("UNSPLASH_ACCESS_KEY")}\"")
    }

    buildTypes {
        debug {
            if (debugAbi != null) {
                ndk {
                    abiFilters += debugAbi
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                // x86 ABIs dropped from release: negligible device share, and their native libs add size.
                abiFilters += debugAbi?.let { listOf(it) } ?: listOf("arm64-v8a", "armeabi-v7a")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Release-like, signed with the debug key, so macrobenchmark can run against it.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
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

// Output file names: Photogether-v1.0-release.aab / Photogether-v1.0-debug.apk
// instead of the default app-release.aab / app-debug.apk.
base {
    archivesName.set("Photogether-v$appVersionName")
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.billing.ktx)
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
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.phosphor.icon)
    implementation(libs.androidx.adaptive)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)

    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":macrobenchmark"))
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
    implementation("androidx.compose.ui:ui-text-google-fonts:1.10.5")
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

    // 3D camera body on the picker screen (glTF/GLB, Filament). 4.x needs AGP 9.1 / compileSdk 37,
    // and 2.2+ is built with Kotlin 2.2 metadata, so 2.1.1 (Kotlin 1.9 stdlib, Filament 1.51) is used.
    // 2.3.0 pins Filament 1.56.0 (first Filament with 16 KB-aligned .so files) and stays on Kotlin 2.0.x.
    implementation("io.github.sceneview:sceneview:2.3.0")
}
