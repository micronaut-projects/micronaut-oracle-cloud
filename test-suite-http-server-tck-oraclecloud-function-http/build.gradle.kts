plugins {
    id("io.micronaut.build.internal.oraclecloud-module")
    id("java-library")
}

dependencies {
    testAnnotationProcessor(platform(mn.micronaut.core.bom))
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(platform(mn.micronaut.core.bom))
    testImplementation(mn.micronaut.inject.java)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.tck)
    testImplementation(libs.junit.platform.engine)
    testImplementation(mn.micronaut.jackson.databind)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(mnLogging.logback.classic)

    testRuntimeOnly(mnValidation.micronaut.validation)

    testImplementation(projects.micronautOraclecloudFunctionHttp)
    testImplementation(projects.micronautOraclecloudFunctionHttpTest)
    testImplementation(libs.fn.runtime)
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
    targetCompatibility = JavaVersion.toVersion("17")
}

micronautBuild {
    binaryCompatibility {
        enabled.set(false)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
