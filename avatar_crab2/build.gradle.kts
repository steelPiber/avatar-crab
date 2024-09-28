// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://repository.map.naver.com/archive/maven")}
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0") // Update your Gradle version as needed
    }
}

