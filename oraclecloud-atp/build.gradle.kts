plugins {
    id("io.micronaut.build.internal.oraclecloud-module")
}

dependencies {
    api(projects.micronautOraclecloudBmcDatabase)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(libs.managed.oracle.jdbc)
    api(mn.micronaut.inject)
    api(mnValidation.micronaut.validation)
    runtimeOnly(libs.slf4j.jcl)

    compileOnly(mnSql.micronaut.jdbc.hikari)
    compileOnly(mnSql.micronaut.jdbc.ucp)
    compileOnly(mnSql.micronaut.jdbc)

    implementation(platform(libs.managed.oracle.jdbc.bom))
    implementation(libs.oracle.security.oraclepki)
    implementation(libs.oracle.security.cert)
    implementation(libs.oracle.security.core)
    implementation(libs.oracle.xml.xdb)
}
