plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.checklist"
    compileSdk = 36

    defaultConfig {
        applicationId = "mx.checklist"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Tu backend
        buildConfigField("String", "BASE_URL", "\"http://3.132.216.201:3000/\"") // termina en /

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            // si quieres logs, etc.
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // para que funcione BuildConfig.BASE_URL
    }

    // Con el plugin kotlin-compose ya no necesitas fijar composeOptions manualmente.
    // composeOptions { kotlinCompilerExtensionVersion = "..." }  // <- déjalo fuera

    compileOptions {
        // Usa 17 porque tu JBR es 21 pero AGP sugiere 17 para compatibilidad anch
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose (usando BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")


    debugImplementation(libs.androidx.ui.tooling)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Red / almacenamiento
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit) // <-- CAMBIADO
    androidTestImplementation(libs.androidx.test.espresso.core) // <-- CAMBIADO


}
