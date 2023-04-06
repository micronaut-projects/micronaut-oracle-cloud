/**
 * This code is responsible for converting the OCI SDK Maven build into a Gradle
 * build. It is neither an accurate conversion nor a generic one: it is only meant
 * to be able to add Micronaut annotation processing.
 */
package io.micronaut.internal.settings

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.extra
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val SDK_GROUP_ID = "com.oracle.oci.sdk"
private const val OCI_SDK_ARTIFACTID_PREFIX = "oci-java-sdk-"
private const val PROJECT_NAME_PREFIX = "oraclecloud-bmc-"

/**
 * This method is called from `settings.gradle.kts` and is the entry
 * point of this module.
 */
@Suppress("unused")
fun Settings.importProjectsAsGradle(checkoutDirectory: File) {
    val builderFactory = DocumentBuilderFactory.newInstance()
    val parentPomFile = File(checkoutDirectory, "pom.xml")
    val parentProperties = parseProperties(builderFactory, parentPomFile)
    val constraints = parentPomFile.inputStream().use {
        val doc = builderFactory.newDocumentBuilder().parse(it)
        parseDependencies(doc.documentElement.findFirst("dependencyManagement")?.findFirst("dependencies"), parentProperties)
    }
    checkoutDirectory.listFiles()
        ?.filter { it.isDirectory }
        ?.forEach { projectDir ->
            importProject(projectDir, parentProperties, builderFactory, constraints)
        }
}

/**
 * Converts a single Maven module into a Gradle project
 */
private fun Settings.importProject(
    projectDir: File,
    parentProperties: Map<String, String>,
    builderFactory: DocumentBuilderFactory,
    constraints: List<Dependency>
) {
    val pomFile = File(projectDir, "pom.xml")
    if (pomFile.exists()) {
        val properties = parentProperties + parseProperties(builderFactory, pomFile)
        val docBuilder = builderFactory.newDocumentBuilder()
        pomFile.inputStream().use {
            val doc = docBuilder.parse(it)
            val modules = doc.documentElement.findFirst("modules")
            if (modules != null) {
                projectDir.listFiles()?.forEach {
                    importProject(it, parentProperties, builderFactory, constraints)
                }
                return
            }
            val parentNode = doc.documentElement.findFirst("parent")
            if (parentNode != null) {
                if (parentNode.parentNode == doc.documentElement) {
                    val parentGroupId = parentNode.findFirst("groupId")?.textContent
                    val parentVersion = parentNode.findFirst("version")?.textContent
                    if (parentGroupId == SDK_GROUP_ID) {
                        val moduleArtifactId = doc.documentElement.findFirst("artifactId")?.textContent
                        if (moduleArtifactId != null) {
                            val moduleName = PROJECT_NAME_PREFIX + moduleArtifactId.substringAfter(OCI_SDK_ARTIFACTID_PREFIX)
                            if (!moduleName.contains("examples") && !moduleName.endsWith("-full")) {
                                include(":$moduleName")
                                project(":$moduleName").setProjectDir(projectDir)
                                configureProject(moduleName, doc, properties, constraints, Dependency.External(parentGroupId, moduleArtifactId, parentVersion, null))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configures a Gradle project, basically applying the plugins
 * which need to be applied. The plugins are regular precompiled
 * script plugins found in `buildSrc`.
 */
private fun Settings.configureProject(
    moduleName: String,
    doc: Document,
    properties: Map<String, String>,
    parentConstraints: List<Dependency>,
    refModule: Dependency.External
) {
    gradle.beforeProject {
        val project = this
        if (project.path == ":$moduleName") {
            // Our build plugins exclude projects which have "doc" in the project name
            // but we want to publish all projects we import
            project.extra.set("micronautPublish", "true")
            pluginManager.apply("io.micronaut.build.internal.ocisdk-metadata-module")
            if (File(projectDir, "src/main/protobuf").exists()) {
                pluginManager.apply("io.micronaut.build.internal.ocisdk-metadata-protobuf")
            }
            val dependencies = parseDependencies(doc.documentElement.findFirst("dependencies"), properties)
            val constraints = parseDependencies(doc.documentElement.findFirst("dependencyManagement")?.findFirst("dependencies"), properties)
            dependencies.forEach {
                if (it.configuration != null) {
                    when (it) {
                        is Dependency.Project -> project.dependencies.add(it.configuration, project.project(it.path))
                        is Dependency.External -> project.dependencies.add(it.configuration, "${it.groupId}:${it.artifactId}:${it.version}")
                    }
                }
            }
            (parentConstraints + constraints).forEach {
                if (it.configuration != null) {
                    when (it) {
                        is Dependency.External -> project.dependencies.constraints.add(it.configuration, "${it.groupId}:${it.artifactId}:${it.version}!!")
                        else -> {}
                    }
                }
            }
            project.configurations.all {
                if (name == "metadataElements") {
                    val bmsDependency = project.dependencies.create("${refModule.groupId}:${refModule.artifactId}:${refModule.version}") as ExternalModuleDependency
                    bmsDependency.exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
                    this.dependencies.add(bmsDependency)
                }
            }
        }
    }
}

/**
 * Parses an XML node and identifies which dependencies are external
 * dependencies vs project dependencies. The heuristic is extremely
 * simple since we use the group id to determine if it's a project
 * dependency.
 */
private fun parseDependencies(dependenciesNode: Node?, properties: Map<String, String>): List<Dependency> {
    val result = mutableListOf<Dependency>()
    dependenciesNode?.forEach {
        val groupId = findFirst("groupId")?.textContent
        val artifactId = findFirst("artifactId")?.textContent
        val version = findFirst("version")?.textContent ?: ""
        val scope = findFirst("scope")?.textContent
        if (groupId != null && artifactId != null) {
            if (groupId == SDK_GROUP_ID && artifactId.startsWith(OCI_SDK_ARTIFACTID_PREFIX)) {
                val projectPath = ":${PROJECT_NAME_PREFIX}${artifactId.substringAfter(OCI_SDK_ARTIFACTID_PREFIX)}"
                result.add(Dependency.Project(projectPath, scope))
            } else {
                val fixedVersion = if (version.startsWith("\${")) {
                    properties.get(version.substring(2, version.length - 1))
                } else {
                    version
                }
                result.add(Dependency.External(groupId, artifactId, fixedVersion, scope))
            }
        }
    }
    return result.toList()
}

/**
 * Parses the `<properties>` block of a Maven build, assuming that properties
 * are declared simply without any other substitution (no recursion).
 */
private fun parseProperties(builderFactory: DocumentBuilderFactory, pomFile: File): Map<String, String> {
    val parentPom = pomFile.inputStream().use {
        builderFactory.newDocumentBuilder().parse(it)
    }
    val propertiesNodes = parentPom.documentElement.findFirst("properties")?.childNodes
    if (propertiesNodes != null) {
        val properties = mutableMapOf<String, String>()
        for (i in 0..propertiesNodes.length) {
            val item = propertiesNodes.item(i)
            if (item != null) {
                properties.put(item.nodeName, item.textContent.trim())
            }
        }
        return properties.toMap()
    }
    return mapOf()
}

/**
 * Finds the first child which name is supplied.
 */
private fun Node.findFirst(name: String): Node? {
    val children = childNodes
    for (i in 0..children.length) {
        val node = children.item(i)
        if (node != null && node.nodeName == name) {
            return node
        }
    }
    return null
}

/**
 * Executes an action on each child
 */
private fun Node.forEach(consumer: Node.() -> Unit) {
    val children = childNodes
    for (i in 0..children.length) {
        val node = children.item(i)
        if (node != null) {
            consumer.invoke(node)
        }
    }
}

/**
 * Model for dependencies, which are either project
 * dependencies or external dependencies.
 */
sealed class Dependency(scope: String?) {
    data class Project(val path: String, val scope: String?) : Dependency(scope)
    data class External(val groupId: String, val artifactId: String, val version: String?, val scope: String?) : Dependency(scope)

    val configuration = when (scope) {
        "compile", "", null -> "api"
        "runtime" -> "runtimeOnly"
        "provided" -> "compileOnly"
        "test" -> "testImplementation"
        else -> null
    }
}
