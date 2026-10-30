buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 ships its own Kotlin compiler, but the bundled KGP can lag behind.
        // Pinning KGP here keeps the Kotlin compiler and the Compose compiler plugin
        // (declared in gradle/libs.versions.toml) on the same version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
