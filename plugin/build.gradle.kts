plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.lnr.t2s"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.lnr.t2s"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        release {
            // Keep plugin annotation, entry point and host API names intact.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":converter"))
    // Host provides the API; never package a second copy into the plugin.
    compileOnly("io.nightfish.lightnovelreader:api:0.4-SNAPSHOT")
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.runtime:runtime")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.nightfish.lightnovelreader:api:0.4-SNAPSHOT")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

// Keep the Android plugin on public Gradle APIs; LNR accepts renamed APK files.
tasks.register<Copy>("packageDebugPlugin") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug")) { include("*.apk") }
    into(layout.buildDirectory.dir("outputs/lnrp"))
    rename { "LightNovelReader-T2S-0.1.0-debug.apk.lnrp" }
}
