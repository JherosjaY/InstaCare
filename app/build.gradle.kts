plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.instacare"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    val localProperties = java.util.Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    val currentsApiKey = localProperties.getProperty("CURRENTS_API_KEY") ?: ""
    val groqApiKey = localProperties.getProperty("GROQ_API_KEY") ?: ""

    defaultConfig {
        applicationId = "com.example.instacare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "CURRENTS_API_KEY", "\"$currentsApiKey\"")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gms.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    
    // Image Cropping
    implementation("com.github.yalantis:ucrop:2.2.9")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // YouTube Player
    implementation(libs.youtube.player)

    // AI Integration
    implementation(libs.okhttp)
    implementation(libs.gson)

    // Premium NumberPicker
    implementation("io.github.ShawnLin013:number-picker:2.4.13")
}