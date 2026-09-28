plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = System.getenv("CARPE_SIGNING_KEYSTORE_PATH").orEmpty()

android {
    namespace = "app.carpe"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.carpe"
        minSdk = 26
        targetSdk = 35
        versionCode = 29
        versionName = "0.29.0"
        val aiEndpoint = project.findProperty("CARPE_AI_ENDPOINT")?.toString() ?: ""
        buildConfigField("String", "CARPE_AI_ENDPOINT", "\"${aiEndpoint}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath.isNotBlank()) {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("CARPE_SIGNING_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("CARPE_SIGNING_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("CARPE_SIGNING_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
