plugins {
    id("io.micronaut.build.internal.oraclecloud-tests")
    id("java-library")
}

dependencies {
    // JUnit 5
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(mnTest.junit.jupiter.engine)

    // Annotation processors
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)

    // Bring ObjectStorage module but exclude Netty HTTP client
    testImplementation(project(":micronaut-oraclecloud-bmc-objectstorage")) {
        exclude(group = "io.micronaut.oraclecloud", module = "micronaut-oraclecloud-httpclient-netty")
    }
    // Use Apache Http Core based HTTP client implementation
    testImplementation(project(":micronaut-oraclecloud-httpclient-apache-http-core"))
    testImplementation(mn.micronaut.http.server.netty)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
