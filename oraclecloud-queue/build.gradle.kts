plugins {
    id("io.micronaut.build.internal.oraclecloud-module")
}

dependencies {
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(projects.micronautOraclecloudBmcQueue)
    api(mn.micronaut.inject)
    api(mn.micronaut.messaging)
    api(mnValidation.micronaut.validation)
}
