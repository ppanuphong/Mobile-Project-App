plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.petcare.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.petcare.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // ./gradlew installDebug -PuseFirebaseEmulator=true → ต่อ Firebase Local Emulator แทนคลาวด์
        val useEmulator = (project.findProperty("useFirebaseEmulator") as String?)?.toBoolean() ?: false
        buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulator.toString())
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // แอปบังคับภาษาไทยเอง จึงต้องเก็บ resource ทุกภาษาไว้ใน App Bundle
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // โหลดรูปสายพันธุ์จาก API
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // อ่านการหมุนของรูปจากกล้อง (Android 8.x)
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    // org.json ใน android.jar เป็นแค่ stub จึงต้องใช้ตัวจริงตอนรัน unit test
    testImplementation(libs.org.json)
}
