plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.smartfiles.core.database"
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
    implementation(project(":core:model"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
