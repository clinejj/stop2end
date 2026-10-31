plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.clinejj.stop2end"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.clinejj.stop2end"
        minSdk = 29
        targetSdk = 37
        // CI passes these: the build number as versionCode, so each build installs as an update,
        // and the release tag (or a dev build label) as versionName.
        versionCode = providers.gradleProperty("appVersionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("appVersionName").getOrElse("dev")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
