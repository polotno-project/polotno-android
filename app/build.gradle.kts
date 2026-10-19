plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.polotno.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.polotno.android"
        minSdk = 29 // MediaStore.Downloads: exports need no storage permission
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.webkit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// The editor bundle is built from web-editor/ and isn't committed.
val checkEditorBundle by tasks.registering {
    val bundle = file("src/main/assets/editor/index.html")
    doLast {
        if (!bundle.exists()) {
            throw GradleException("Missing editor bundle. Run `npm install && npm run build:android` in web-editor/ first.")
        }
    }
}
tasks.named("preBuild") { dependsOn(checkEditorBundle) }
