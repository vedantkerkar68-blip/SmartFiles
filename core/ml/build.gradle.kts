plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smartfiles.core.ml"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.pdfbox.android)
    implementation(libs.litert)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
