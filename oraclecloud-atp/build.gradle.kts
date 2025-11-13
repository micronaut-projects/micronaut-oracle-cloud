plugins {
    id("io.micronaut.build.internal.oraclecloud-module")
}

dependencies {
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(mn.micronaut.inject)
    api(mnSql.ojdbc11)
    api(mnValidation.micronaut.validation)
    api(projects.micronautOraclecloudBmcDatabase)
    compileOnly(mnSql.micronaut.jdbc)
    compileOnly(mnSql.micronaut.jdbc.hikari)
    compileOnly(mnSql.micronaut.jdbc.ucp)
    implementation(libs.oracle.security.cert)
    implementation(libs.oracle.security.core)
    implementation(libs.oracle.security.oraclepki)
    implementation(libs.oracle.xml.xdb)
    implementation(platform("${libs.oracle.jdbc.bom.get()}:${mnSql.versions.ojdbc.get()}"))
    runtimeOnly(libs.slf4j.jcl)
    testImplementation(mnTest.junit.jupiter.params)
}
