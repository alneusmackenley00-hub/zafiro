plugins {
    id("com.android.library") version "9.1.1"
}

android {
    namespace = "com.termux.terminal"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
