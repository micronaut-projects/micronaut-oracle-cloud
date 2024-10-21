import io.micronaut.internal.settings.importProjectsAsGradle
import me.champeau.gradle.igp.gitRepositories
import org.tomlj.Toml

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("settings-build-logic")
}

buildscript {
    configurations {
        classpath {
            resolutionStrategy {
                eachDependency {
                    if (requested.group == "org.eclipse.jgit") {
                        useVersion("5.13.0.202109080827-r")
                    }
                }
            }
        }
    }
}

plugins {
    id("io.micronaut.build.shared.settings") version "7.2.0"
    id("me.champeau.includegit") version "0.1.6"
    id("io.micronaut.build.internal.ocisdk")
}

rootProject.name = "oracle-cloud"

include("oraclecloud-atp")
include("oraclecloud-atp-hikari-test")
include("oraclecloud-atp-ucp-test")
include("oraclecloud-bom")
include("oraclecloud-certificates")
include("oraclecloud-common")
include("oraclecloud-function")
include("oraclecloud-function-http")
include("oraclecloud-function-http-test")
include("oraclecloud-httpclient-apache-http-core")
include("oraclecloud-httpclient-netty")
include("oraclecloud-serde-processor")
include("oraclecloud-logging")
include("oraclecloud-micrometer")
include("oraclecloud-oke-workload-identity")
include("oraclecloud-sdk-base")
include("oraclecloud-sdk")
include("oraclecloud-sdk-rxjava2")
include("oraclecloud-sdk-reactor")
include("oraclecloud-sdk-processor")
include("oraclecloud-vault")
include("docs-examples:example-java")
include("docs-examples:example-groovy")
include("docs-examples:example-kotlin")
include("docs-examples:example-function-java")
include("docs-examples:example-function-groovy")
include("docs-examples:example-function-kotlin")
include("docs-examples:example-http-function-java")
include("docs-examples:example-http-function-groovy")
include("docs-examples:example-http-function-kotlin")
include("test-suite-java")
include("test-suite-graal-function")
include("test-suite-graal-logging")
include("test-suite-http-server-tck-oraclecloud-function-http")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

configure<io.micronaut.build.MicronautBuildSettingsExtension> {
    useStandardizedProjectNames.set(true)
    importMicronautCatalog()
    importMicronautCatalog("micronaut-groovy")
    importMicronautCatalog("micronaut-kotlin")
    importMicronautCatalog("micronaut-micrometer")
    importMicronautCatalog("micronaut-reactor")
    importMicronautCatalog("micronaut-rxjava2")
    importMicronautCatalog("micronaut-serde")
    importMicronautCatalog("micronaut-servlet")
    importMicronautCatalog("micronaut-sql")
    importMicronautCatalog("micronaut-validation")
    importMicronautCatalog("micronaut-discovery-client")
}

val libs = Toml.parse(File(rootProject.projectDir.absoluteFile, "gradle/libs.versions.toml").toPath())!!

gitRepositories {
    // Checkout in a directory that is ignored by the sync'ed .gitignore
    checkoutsDirectory.set(file("target/checkouts"))
    include("oci-java-sdk") {
        uri.set("https://github.com/oracle/oci-java-sdk.git")
        autoInclude.set(false)
        branch.set("master")
        tag.set("v" + libs.getString("versions.oci"))
        codeReady {
            importProjectsAsGradle(checkoutDirectory)
        }
    }
}
