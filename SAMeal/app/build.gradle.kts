import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val buildTimestamp: String = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "xyz.jo6r.sameal"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.jo6r.sameal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.5-$buildTimestamp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        applicationVariants.all {
            outputs.all {
                val outputImpl = this as BaseVariantOutputImpl
                val newName = "SAMeal-${versionName}.apk"
                outputImpl.outputFileName = newName
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
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}