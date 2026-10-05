plugins {
    id("io.micronaut.build.internal.oraclecloud-tests")
    id("java-library")
    id("io.micronaut.build.internal.java-base")
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
        projects.micronautOraclecloudBmcStreaming,
        projects.micronautOraclecloudBmcFunctions,
        projects.micronautOraclecloudBmcEncryption
    ).forEach { implementation(it) }

    testImplementation(libs.oci.common.httpclient.jersey3)
    // Netty's self-signed certificate generator needs Bouncy Castle on Java 25.
    testRuntimeOnly("org.bouncycastle:bcpkix-jdk15on:1.70")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
