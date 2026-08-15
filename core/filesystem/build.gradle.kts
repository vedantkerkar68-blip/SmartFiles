plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smartfiles.core.filesystem"
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
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
