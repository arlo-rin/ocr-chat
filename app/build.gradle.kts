plugins {
    id("com.android.application")
}

android {
    namespace = "app.sidereply"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.sidereply"
        minSdk = 30
        targetSdk = 36
        versionCode = 19
        versionName = "0.4.3-root"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // No on-device ML: the screenshot goes to the configured vision model instead.
    implementation("com.google.android.material:material:1.14.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
