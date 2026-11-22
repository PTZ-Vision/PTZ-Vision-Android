import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinCompose)
}

// Optional NDI® SDK for Android (see docs/NDI.md). Only its lib/<abi>/libndi.so is packaged.
val ndiSdkLibDir: File? = run {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
    (props.getProperty("ndi.sdk.dir") ?: System.getenv("NDI_SDK_DIR"))?.let { File(it, "lib") }
}

android {
    namespace = "it.mobile.bisax.ptzvision"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "it.mobile.bisax.ptzvision"
        minSdk = 29 // Android 10
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
        buildConfigField("boolean", "NDI_RUNTIME_BUNDLED", ndiSdkLibDir?.exists()?.toString() ?: "false")
    }

    ndkVersion = "30.0.16248370"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    sourceSets {
        getByName("main") {
            // NDI runtime (libndi.so, proprietary, NOT committed): taken from the NDI SDK for
            // Android pointed to by `ndi.sdk.dir` in local.properties or the NDI_SDK_DIR env var.
            ndiSdkLibDir?.takeIf { it.exists() }?.let { jniLibs.directories += it.absolutePath }
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("../keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "ptzvision"
                keyAlias = System.getenv("KEY_ALIAS") ?: "ptzvision"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "ptzvision"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystoreFile = file("../keystore.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // android.util.Log & co. return default values instead of throwing in JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
        jniLibs {
            // The NDI file recorder/player library is not used by the app.
            excludes += "**/libndifile.so"
        }
    }
}

kotlin {
    // Compila con JDK 25 (scaricato automaticamente da foojay se manca).
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.material3.window)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.apache.httpcomponents.httpclient)
    implementation(libs.rtsp.client.android)
    implementation(libs.coil.compose)
}

