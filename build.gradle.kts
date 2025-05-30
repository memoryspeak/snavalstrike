plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.snavalstrike.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.snavalstrike.app"
        minSdk = 19
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1") // 1.6.1
    implementation("androidx.recyclerview:recyclerview:1.1.0") // 1.1.0
    testImplementation("junit:junit:4.13.2") // 4.13.2
    androidTestImplementation("androidx.test.ext:junit:1.1.3") // 1.1.3
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0") // 3.4.0
}