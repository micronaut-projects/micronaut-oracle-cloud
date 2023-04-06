import io.micronaut.internal.settings.importProjectsAsGradle
import me.champeau.gradle.igp.gitRepositories

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
    id("io.micronaut.build.shared.settings") version "5.4.8"
    id("me.champeau.includegit") version "0.1.6"
    id("io.micronaut.build.internal.ocisdk")
}

val ociVersion = providers.gradleProperty("ociVersion").get()

(gradle as ExtensionAware).extra.set("ociBom", groovy.xml.XmlSlurper().parse(
    "https://repo.maven.apache.org/maven2/com/oracle/oci/sdk/oci-java-sdk-bom/$ociVersion/oci-java-sdk-bom-${ociVersion}.pom"))

rootProject.name = "oracle-cloud"

if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11)) {
    include("oraclecloud-atp")
    include("oraclecloud-atp-hikari-test")
    include("oraclecloud-atp-ucp-test")
}
include("oraclecloud-bom")
include("oraclecloud-common")
include("oraclecloud-function")
include("oraclecloud-function-http")
include("oraclecloud-function-http-test")
include("oraclecloud-httpclient-netty")
include("oraclecloud-httpclient-netty-processor")
include("oraclecloud-httpnetty-client-processor")
include("oraclecloud-logging")
include("oraclecloud-micrometer")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
    versionCatalogs {
        create("mn") {
            from("io.micronaut:micronaut-bom:${providers.gradleProperty("micronautVersion").get()}")
        }
    }
}

gitRepositories {
    include("oci-java-sdk") {
        uri.set("https://github.com/oracle/oci-java-sdk.git")
        autoInclude.set(false)
        branch.set("master")
        codeReady {
            importProjectsAsGradle(checkoutDirectory)
        }
    }
}
