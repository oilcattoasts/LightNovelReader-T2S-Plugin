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
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.3"
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        // Must match the host's ABI: BookInformation.getLastUpdated returns
        // j$.time.LocalDateTime in its DEX, even on Android 8+ devices.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        assets.srcDir("../plugin/src/main/assets")
        manifest.srcFile("../plugin/src/main/AndroidManifest.xml")
    }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":converter"))
    compileOnly("io.nightfish.lightnovelreader:api:0.3")
    // Match the 1.2.3d host, rather than the dev/1.3 Compose runtime.
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.material3:material3:1.5.0-alpha22")
    implementation("androidx.compose.foundation:foundation-layout:1.11.3")
    implementation("androidx.compose.runtime:runtime:1.11.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.nightfish.lightnovelreader:api:0.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
tasks.register<Copy>("packageDebugPlugin") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug")) { include("*.apk") }
    into(layout.buildDirectory.dir("outputs/lnrp"))
    rename { "LightNovelReader-T2S-0.1.3-api3-debug.apk.lnrp" }
}
