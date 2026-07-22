plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.royals.airescape"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file("D:\\java\\Key\\airescape")
            storePassword = "8750257510"
            keyAlias = "key0"
            keyPassword = "8750257510"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.royals.airescape"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        debug {
            // Google test Ad IDs — safe for development
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            // Real Ad IDs — production only
            manifestPlaceholders["admobAppId"] = "ca-app-pub-1811294933992844~3548299390"
            buildConfigField("String", "BANNER_ID", "\"ca-app-pub-1811294933992844/4294404854\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"ca-app-pub-1811294933992844/5460581637\"")
            buildConfigField("String", "REWARDED_ID", "\"ca-app-pub-1811294933992844/9946186782\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.gms:play-services-games-v2:20.1.2")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")
}
