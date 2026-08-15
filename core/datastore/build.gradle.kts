plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smartfiles.core.datastore"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
