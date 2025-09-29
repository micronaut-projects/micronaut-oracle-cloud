plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
