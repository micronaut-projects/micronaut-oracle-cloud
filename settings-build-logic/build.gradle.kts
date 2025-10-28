import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
