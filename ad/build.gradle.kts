plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.allever.android.lib.ad"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    buildFeatures {
        buildConfig = true
        dataBinding = false
        viewBinding = true
    }
}

dependencies {
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    //coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    implementation ("com.google.android.ump:user-messaging-platform:4.0.0")
    api ("com.google.android.gms:play-services-ads:25.5.0")
    implementation ("com.google.ads.mediation:ironsource:9.7.0.0")
    implementation ("com.google.ads.mediation:unity:4.12.1.0")
    implementation ("com.google.android.material:material:1.10.0")

    implementation ("com.google.code.gson:gson:2.10")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    //coil
    implementation("io.coil-kt:coil:2.4.0")

}