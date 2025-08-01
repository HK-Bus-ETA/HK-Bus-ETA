/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleDevToolsKsp)
    alias(libs.plugins.compose.compiler)
}

composeCompiler {
    targetKotlinPlatforms = targetKotlinPlatforms.get() - setOf(KotlinPlatformType.native)
}

kotlin {
    androidTarget {
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    listOf(
        watchosSimulatorArm64(),
        watchosX64(),
        watchosArm64(),
        watchosArm32(),
        watchosDeviceArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(compose.runtime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(compose.runtime)
        }
        watchosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        desktopMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(compose.runtime)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(compose.runtime)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.encoding)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.stately.concurrency)
            implementation(libs.stately.concurrent.collections)
            implementation(libs.xmlCore)
            implementation(libs.serialization.xml)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
    }
}

android {
    namespace = "com.loohp.hkbuseta.common"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
