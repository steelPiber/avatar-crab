plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.avatar_crab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.avatar_crab"
        minSdk = 30
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    kapt {
        generateStubs = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.camera.core)
    implementation(project(":opencv"))
    implementation(project(":opencv"))
    implementation(libs.firebase.auth.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("androidx.core:core-ktx:1.6.0")
    implementation ("androidx.appcompat:appcompat:1.3.1")
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation ("androidx.activity:activity-ktx:1.9.2")
    debugImplementation ("androidx.compose.ui:ui-tooling:1.6.7")
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4:1.6.7")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4") // 추가

    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.fragment:fragment-ktx:1.7.1")
    implementation ("androidx.work:work-runtime-ktx:2.9.0")

    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.2.0")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.squareup.picasso:picasso:2.8")

    //데이터 저장 room 라이브러리
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation ("com.google.code.gson:gson:2.8.6")

    implementation ("androidx.databinding:databinding-runtime:7.0.4")

    implementation ("androidx.camera:camera-camera2:1.3.4")
    implementation ("androidx.camera:camera-lifecycle:1.3.4")
    implementation ("androidx.camera:camera-view:1.3.4")

    implementation ("com.naver.maps:map-sdk:3.19.1")


}