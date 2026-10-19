import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Read local development credentials from local.properties. They may be useful
// in debug builds, but release builds below explicitly blank every long-lived
// provider secret so a distributable APK can never inherit a developer key.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = (localProps.getProperty(key) ?: "").trim()

fun javaStringLiteral(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile")?.isNotBlank() == true
val buildVersionCode = providers.gradleProperty("ALLO_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 30
val buildVersionName = providers.gradleProperty("ALLO_VERSION_NAME")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "3.10.0"

android {
    namespace = "com.kenza.callsim"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kenza.callsim"
        minSdk = 26
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = buildVersionName

        buildConfigField("String", "ELEVENLABS_AGENT_ID", javaStringLiteral(secret("ELEVENLABS_AGENT_ID")))
        buildConfigField("String", "ELEVENLABS_API_KEY", javaStringLiteral(secret("ELEVENLABS_API_KEY")))
        buildConfigField("String", "ELEVENLABS_VOICE_ID", javaStringLiteral(secret("ELEVENLABS_VOICE_ID")))
        buildConfigField("String", "GEMINI_API_KEY", javaStringLiteral(secret("GEMINI_API_KEY")))
        buildConfigField("String", "GEMINI_TOKEN_BROKER_URL", javaStringLiteral(secret("GEMINI_TOKEN_BROKER_URL")))
        buildConfigField("String", "CONTACT_NAME", javaStringLiteral(secret("CONTACT_NAME").ifEmpty { "Kenza" }))
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            buildConfigField("String", "ELEVENLABS_API_KEY", "\"\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
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

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json classes are framework stubs in local JVM tests. Use the reference
    // implementation only on the test classpath so production serialization can be round-tripped.
    testImplementation("org.json:json:20260814")
}
