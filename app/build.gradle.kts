plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pt.upt.studenthub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pt.upt.studenthub.a51856"
        minSdk = 24
        // 34 evita o modo "edge-to-edge" obrigatÃ³rio do Android 15+ (conteÃºdo por baixo das barras)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Step 1 â€” ViewModel e LiveData (Week 6)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
