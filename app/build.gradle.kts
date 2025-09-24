plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.hello"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hello"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.firebase:firebase-auth:23.0.0")
    implementation ("com.google.android.gms:play-services-auth:21.4.0")

    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("com.airbnb.android:lottie:6.0.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    // Add the SQLite dependencies
    implementation ("androidx.sqlite:sqlite:2.4.0")
    implementation ("androidx.sqlite:sqlite-framework:2.4.0")
    // Firebase
    implementation ("com.google.firebase:firebase-bom:32.1.0")
    implementation ("com.google.firebase:firebase-auth")

    implementation ("com.squareup.okhttp3:okhttp:4.9.0" )  // For networking
    implementation ("org.simpleframework:simple-xml:2.7.1") // For XML parsing

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.firebase.functions)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(project(":app"))
    annotationProcessor (libs.compiler)
    // Other dependencies
    implementation (libs.androidx.appcompat.v140)
    implementation (libs.androidx.viewpager2)
    implementation (libs.dotsindicator)
    implementation (libs.tbuonomo.dotsindicator)
    implementation (libs.recyclerview)
    implementation (libs.material.v140)
    implementation (libs.androidx.constraintlayout.v212)
    implementation (libs.material.v1100)
    implementation (libs.com.github.bumptech.glide.glide)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation (libs.firebase.messaging)
    implementation (libs.firebase.messaging.v2312)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
