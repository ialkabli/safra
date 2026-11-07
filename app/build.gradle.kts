plugins { id("com.android.application") }
android {
 namespace = "com.iprigner.safra"
 compileSdk { version = release(37) }
 defaultConfig { applicationId = "com.iprigner.safra"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0"; testInstrumentationRunner = "com.iprigner.safra.AppTests" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildTypes { release { isMinifyEnabled = false } }
}
