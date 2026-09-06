// Matrix co-protocol spike module (#129, master plan #128).
// Isolated on purpose: everything Matrix lives here during P0; SimpleX code is
// untouched and release builds do not depend on this module (debug-only wiring
// in :android until the P0 gates in #132 pass).
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "chat.simplex.matrix"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    // matrix-rust-sdk Kotlin bindings (UniFFI), the engine powering Element X.
    // Version pinned to an existing Maven Central artifact; the distribution
    // repo's git tags are broken, only trust the maven-metadata list.
  api("org.matrix.rustcomponents:sdk-android:26.09.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
  // Display model access for the Room->ChatInfo adapter (#135) and
  // chatsContext ingestion (#136). Read-only usage; SimpleXAPI.kt untouched.
  implementation(project(":common"))
  testImplementation("junit:junit:4.13.2")
}
