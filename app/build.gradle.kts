import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
}

val paymentLocalProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val paymentApiBaseUrl = providers.gradleProperty("PAYMENT_API_BASE_URL")
    .orElse(providers.environmentVariable("PAYMENT_API_BASE_URL"))
    .orElse(paymentLocalProperties.getProperty("PAYMENT_API_BASE_URL", ""))
val paymentOfflineDemo = providers.gradleProperty("PAYMENT_DEMO").map { it.toBoolean() }.orElse(false).get()
// Sandbox merchant settings stay in ignored local files and are never included in release APKs.
val vnpayLocal = Properties().apply {
    rootProject.file("payment-server/.env").takeIf { it.isFile }?.reader()?.use { load(it) }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val clientSandbox = providers.gradleProperty("VNPAY_CLIENT_SANDBOX").map { it.toBoolean() }.orElse(true).get() && !paymentOfflineDemo

android {
    namespace = "com.example.tmdt"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.tmdt"
        minSdk = 29
        targetSdk = 36
        versionCode = 10
        versionName = "1.2.0"
        buildConfigField("String", "PAYMENT_API_BASE_URL", "\"" + paymentApiBaseUrl.get().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "VNPAY_CLIENT_SANDBOX", clientSandbox.toString())
            buildConfigField("String", "VNPAY_TMN_CODE", quoted(if (clientSandbox) vnpayLocal.getProperty("VNPAY_TMN_CODE", "") else ""))
            buildConfigField("String", "VNPAY_HASH_SECRET", quoted(if (clientSandbox) vnpayLocal.getProperty("VNPAY_HASH_SECRET", "") else ""))
            if (paymentOfflineDemo) {
                applicationIdSuffix = ".paymentdemo"
                versionNameSuffix = "-demo"
            }
        }
        release {
            buildConfigField("boolean", "VNPAY_CLIENT_SANDBOX", "false")
            buildConfigField("String", "VNPAY_TMN_CODE", "\"\"")
            buildConfigField("String", "VNPAY_HASH_SECRET", "\"\"")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    if (paymentOfflineDemo) {
        sourceSets.getByName("debug") {
            manifest.srcFile("src/paymentDemo/AndroidManifest.xml")
            java.srcDir("src/paymentDemo/java")
            res.srcDir("src/paymentDemo/res")
        }
        sourceSets.maybeCreate("testDebug").java.srcDir("src/paymentDemoUnitTest/java")
        sourceSets.getByName("androidTest").java.srcDir("src/paymentDemoAndroidTest/java")
    }
}

// The offline APK has a separate package and does not initialize or configure Firebase.
if (paymentOfflineDemo) {
    tasks.matching { it.name == "processDebugGoogleServices" }.configureEach { enabled = false }
}

dependencies {
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
