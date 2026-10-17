plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.bloodflow.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.bloodflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true; buildConfig = true }
    val firebaseVariables = mapOf(
        "FIREBASE_API_KEY" to "BLOODFLOW_FIREBASE_API_KEY",
        "FIREBASE_APP_ID" to "BLOODFLOW_FIREBASE_APP_ID",
        "FIREBASE_PROJECT_ID" to "BLOODFLOW_FIREBASE_PROJECT_ID",
        "FIREBASE_WEB_CLIENT_ID" to "BLOODFLOW_FIREBASE_WEB_CLIENT_ID"
    )
    defaultConfig {
        firebaseVariables.forEach { (field, env) ->
            val value = System.getenv(env).orEmpty()
            buildConfigField("String", field, "\"$value\"")
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation(platform("com.google.firebase:firebase-bom:35.0.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
