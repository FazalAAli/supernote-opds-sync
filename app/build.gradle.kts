plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.fazalaali.supernoteopds"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.fazalaali.supernoteopds"
        minSdk = 29
        // Supernote runs Android 11. Targeting 29 keeps legacy storage (requestLegacyExternalStorage),
        // so a normal runtime prompt grants write access to Document/ instead of "All files access".
        targetSdk = 29
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing comes from env vars (set by the release workflow). Without them,
    // assembleRelease still works but produces an unsigned APK.
    val keystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (keystore != null) create("release") {
            storeFile = file(keystore)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        // targetSdk 29 is deliberate (see above).
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    testImplementation("junit:junit:4.13.2")
    // android.jar's XmlPullParser is a stub on the JVM; kxml2 is the implementation Android itself ships.
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}
