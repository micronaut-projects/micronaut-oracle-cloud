plugins {
    id("io.micronaut.build.internal.oraclecloud-tests")
    id("java-library")
}

dependencies {
    testAnnotationProcessor(platform(mn.micronaut.core.bom))
    testImplementation(libs.fn.runtime)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.tck)
    testImplementation(mn.micronaut.inject.java)
    testImplementation(mn.micronaut.jackson.databind)
    testImplementation(mnTest.junit.platform.engine)
    testImplementation(mnTest.junit.platform.suite)
    testImplementation(platform(mn.micronaut.core.bom))
    testImplementation(projects.micronautOraclecloudFunctionHttp)
    testImplementation(projects.micronautOraclecloudFunctionHttpTest)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnValidation.micronaut.validation)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
