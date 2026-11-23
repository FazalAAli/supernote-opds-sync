plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.fazalaali.supernoteopds"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.fazalaali.supernoteopds"
        minSdk = 30
        // Supernote runs Android 11. Higher targets only add restrictions we don't need.
        targetSdk = 30
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        // targetSdk 30 is deliberate (see above).
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    testImplementation("junit:junit:4.13.2")
    // android.jar's XmlPullParser is a stub on the JVM; kxml2 is the implementation Android itself ships.
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}
