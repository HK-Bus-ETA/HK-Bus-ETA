/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.loohp.hkbuseta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.loohp.hkbuseta"
        minSdk = 30
        targetSdk = 33
        versionCode = 362
        versionName = "1.7.1"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"

        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.buildDir.absolutePath + "/compose_metrics")
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination="  + project.buildDir.absolutePath + "/compose_metrics")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    sourceSets {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")
        }
    }
}

dependencies {
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("androidx.core:core-splashscreen:1.1.0-alpha02")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.wear:wear-ongoing:1.1.0-alpha01")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.wear.tiles:tiles:1.3.0-alpha02")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("androidx.wear:wear-input-testing:1.1.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")
    implementation("androidx.wear:wear-phone-interactions:1.0.1")
    implementation("androidx.wear:wear-remote-interactions:1.0.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.1.0-alpha02")
    implementation("androidx.wear.protolayout:protolayout:1.1.0-alpha02")
    implementation("androidx.wear.protolayout:protolayout-material:1.1.0-alpha02")
    implementation("io.github.aghajari:AnnotatedText:1.0.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("me.saket.telephoto:zoomable:0.5.0")
    implementation("com.google.android.horologist:horologist-compose-layout:0.5.10")
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

apply(plugin = "com.google.gms.google-services")