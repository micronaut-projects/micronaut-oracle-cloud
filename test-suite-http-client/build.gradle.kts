plugins {
    id("io.micronaut.build.internal.oraclecloud-tests")
    id("java-library")
}

dependencies {
    api(mn.netty.codec.http)

    implementation(mnTest.junit.jupiter.api)
    implementation(mnTest.junit.jupiter.engine)

    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mnSerde.micronaut.serde.processor)

    listOf(
        projects.micronautOraclecloudBmcMonitoring,
        projects.micronautOraclecloudBmcIdentity,
        projects.micronautOraclecloudBmcObjectstorage,
        projects.micronautOraclecloudBmcKeymanagement,
        projects.micronautOraclecloudBmcSecrets,
        projects.micronautOraclecloudBmcVault,
        projects.micronautOraclecloudBmcLogging,
        projects.micronautOraclecloudBmcLoggingingestion,
        projects.micronautOraclecloudBmcLoggingsearch,
        projects.micronautOraclecloudBmcStreaming
    ).forEach { implementation(it) }
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
    targetCompatibility = JavaVersion.toVersion("17")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
