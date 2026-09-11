@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int
val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion

android {
    namespace = "com.functy.fewards"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.functy.fewards"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = rootProject.extra["fewardsVersionCode"] as Int
        versionName = rootProject.extra["fewardsVersionName"] as String
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            optimization.enable = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            optimization.enable = false
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.hiddenapibypass)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.squircle)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.material.kolor)
    implementation(libs.zxing.core)

    implementation(libs.androidx.webkit)
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
