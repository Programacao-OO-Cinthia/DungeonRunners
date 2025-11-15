plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dungeonrunnersapp"
    compileSdk = 34 // Mantemos 34 por enquanto

    defaultConfig {
        applicationId = "com.example.dungeonrunnersapp"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    // --- SUPABASE VIA HTTP ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")

    // --- ANDROIDX BÁSICO (VERSÕES COMPATÍVEIS) ---
    implementation("androidx.appcompat:appcompat:1.6.1") // ✅ Versão compatível
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.8.2") // ✅ Versão compatível com SDK 34

    // --- MATERIAL DESIGN ---
    implementation("com.google.android.material:material:1.9.0") // ✅ Versão estável

    // --- GOOGLE PLAY SERVICES ---
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- CARDVIEW ---
    implementation("androidx.cardview:cardview:1.0.0")

    // --- TESTES ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}