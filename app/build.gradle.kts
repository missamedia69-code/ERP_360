plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0 intègre Kotlin ; seul le plugin du compilateur Compose est nécessaire
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.missa.b360"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.missa.b360"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Export du schéma Room pour les migrations (livrable §12 : migrations)
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room — base de données offline (KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore — réglages + verrous d'amont
    implementation(libs.androidx.datastore.preferences)

    // Hilt — injection de dépendances
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Hilt + WorkManager (@HiltWorker / HiltWorkerFactory)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // AppCompat — per-app language (5 langues, RTL arabe)
    implementation(libs.androidx.appcompat)

    // WorkManager — sauvegarde auto + purge journal 12 mois
    implementation(libs.androidx.work.runtime)

    // Sécurité — hash PIN (jamais en clair)
    implementation(libs.androidx.security.crypto)

    // Export JSON (kotlinx-serialization)
    implementation(libs.kotlinx.serialization.json)

    // Tests unitaires des règles métier (UseCases)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
}
