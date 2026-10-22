import java.util.Properties
import java.net.URI

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
// VNPay TEST settings belong to the Android app, independent of the optional payment backend.
val vnpayLocal = Properties().apply {
    rootProject.file("vnpay.properties").takeIf { it.isFile }?.reader(Charsets.UTF_8)?.use { load(it) }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val clientSandbox = providers.gradleProperty("VNPAY_CLIENT_SANDBOX").map { it.toBoolean() }.orElse(true).get()
val vnpayReturnUrl = vnpayLocal.getProperty("VNPAY_RETURN_URL", "https://tmdt.example/vnpay/return")

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
        versionCode = 11
        versionName = "1.2.1"
        buildConfigField("String", "PAYMENT_API_BASE_URL", "\"" + paymentApiBaseUrl.get().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "VNPAY_CLIENT_SANDBOX", clientSandbox.toString())
            buildConfigField("String", "VNPAY_TMN_CODE", quoted(if (clientSandbox) vnpayLocal.getProperty("VNPAY_TMN_CODE", "") else ""))
            buildConfigField("String", "VNPAY_HASH_SECRET", quoted(if (clientSandbox) vnpayLocal.getProperty("VNPAY_HASH_SECRET", "") else ""))
            buildConfigField("String", "VNPAY_RETURN_URL", quoted(vnpayReturnUrl))
        }
        release {
            buildConfigField("boolean", "VNPAY_CLIENT_SANDBOX", "false")
            buildConfigField("String", "VNPAY_TMN_CODE", "\"\"")
            buildConfigField("String", "VNPAY_HASH_SECRET", "\"\"")
            buildConfigField("String", "VNPAY_RETURN_URL", "\"\"")
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
}

val checkVnpayConfig = tasks.register("checkVnpayConfig") {
    group = "verification"
    description = "Validate native VNPay TEST settings without printing merchant secrets."
    doLast {
        if (clientSandbox) {
            check(vnpayLocal.getProperty("VNPAY_TMN_CODE", "").matches(Regex("[A-Za-z0-9]{8}"))) { "Fill VNPAY_TMN_CODE in root vnpay.properties (see vnpay.properties.example)." }
            check(vnpayLocal.getProperty("VNPAY_HASH_SECRET", "").length >= 16) { "Fill VNPAY_HASH_SECRET in root vnpay.properties." }
            val callback = URI(vnpayReturnUrl)
            check(callback.scheme == "https" && callback.host != null && callback.userInfo == null && callback.port == -1 && callback.query == null && callback.fragment == null && callback.path.isNotEmpty()) { "VNPAY_RETURN_URL must be an HTTPS URL without query or fragment." }
            logger.lifecycle("VNPay TEST configuration valid. Native WebView callback; no backend URL required.")
        }
    }
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(checkVnpayConfig) }

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
