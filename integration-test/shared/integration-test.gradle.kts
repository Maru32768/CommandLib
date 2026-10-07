import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URL
import java.nio.file.Files

/*
 * Shared Gradle script for Bukkit-family integration-test target plugins.
 *
 * This script is designed for Bukkit-compatible server platforms (Paper, Mohist, Spigot). It uses
 * the Shadow plugin to inline CommandLib sources directly into the test plugin jar. Non-Bukkit
 * platforms (Forge, Fabric, Velocity, NeoForge) require a separate shared script because their
 * build toolchains differ substantially.
 *
 * Each target plugin's build.gradle.kts defines only version-specific differences via extra
 * properties, then calls:
 *   apply(from = "../../../shared/integration-test.gradle.kts")
 *
 * Required properties:
 *
 * - platformDependencies:
 *   Pipe-separated list of compileOnly dependency notations for the target server API.
 *   Use multiple entries when the platform requires more than one API artifact at compile time.
 *   Examples:
 *     "io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT"
 *     "io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT|com.example:some-api:1.0"
 *
 * - javaVersion: Java toolchain version as a stringified integer.
 *
 * - platform: Runtime server family. Current values: "paper", "mohist".
 *   This describes the server executable, not the CommandLib module.
 *   - "mohist" uses a Mohist server jar but compiles against spigot module sources.
 *   - "paper" uses a Paper server jar and compiles against spigot module sources by default.
 *
 * - minecraftServerVersion:
 *   Minecraft version of the server, used to pick plugin downloads such as PlugManX. This is
 *   separate from the API dependency because some fixtures intentionally use names such as
 *   test-plugin-1.21.0 while Paper stores jars under 1.21.
 *
 * - serverJarDownloads:
 *   Format: "<url>=><relative path>" entries joined by "|".
 *   Example:
 *   "https://.../paper.jar=>server/server.jar|https://.../paper-mojmap.jar=>server_mojmap/server.jar"
 *   Parsing rules:
 *   - First split by "|" into download entries.
 *   - Then split each entry by "=>" into URL and destination path.
 *   - Destination path is resolved relative to the test plugin directory.
 *
 * Optional properties:
 *
 * - commandlibModule:
 *   Name of the CommandLib module whose sources are included in the test plugin. This corresponds
 *   to a directory at the repository root. Bukkit-family targets normally use "spigot" or "paper".
 *   With "spigot", the typed NMS jars that :spigot:collectTypedNms writes to spigot/build/typed-nms
 *   are bundled too, as the published spigot jar bundles them.
 *
 * - testPluginSuite:
 *   Shared Bukkit-family test plugin suite to compile: "spigot" or "paper".
 *   Defaults to "paper" when commandlibModule is "paper"; otherwise "spigot".
 *
 * - copyTargets:
 *   Format: "<relative dir>|<relative dir>|..."
 *   Default: "server/plugins"
 *   Example:
 *   "server/plugins|server_mojmap/plugins"
 *
 * - includeProtocolLib:
 *   "true" to register copyProtocolLibToServer.
 *
 * - pluginJarDownloads:
 *   Format: "<url>=><file name>" entries joined by "|".
 *   Each entry is downloaded into every configured copyTargets directory.
 *
 * - autoReloaderJarDownloadUrl:
 *   Override URL for AutoReloader-1.1.0.jar. It can also be supplied with the
 *   COMMANDLIB_AUTORELOADER_JAR_URL environment variable.
 *
 * Minimal example:
 *   extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT"
 *   extra["javaVersion"] = "17"
 *   extra["platform"] = "paper"
 *   extra["minecraftServerVersion"] = "1.20.4"
 *   extra["serverJarDownloads"] =
 *       "https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/496/downloads/paper-1.20.4-496.jar=>server/server.jar"
 *   apply(from = "../../../shared/integration-test.gradle.kts")
 */

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
    }
}

apply(plugin = "java")
apply<ShadowPlugin>()

fun Project.requiredStringProperty(name: String): String =
    findProperty(name)?.toString() ?: error("Missing project property: $name")

fun Project.requiredListProperty(name: String): List<String> =
    requiredStringProperty(name)
        ?.split("|")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?: error("Missing project property: $name")

fun Project.optionalStringProperty(name: String): String? = findProperty(name)?.toString()

fun Project.optionalBooleanProperty(name: String): Boolean =
    optionalStringProperty(name)?.equals("true", ignoreCase = true) == true

fun Project.optionalListProperty(name: String): List<String> =
    optionalStringProperty(name)
        ?.split("|")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()

val platformDependencies: List<String> = project.requiredListProperty("platformDependencies")
val javaVersion = project.requiredStringProperty("javaVersion").toInt()
val platform = project.requiredStringProperty("platform")
val minecraftServerVersion = project.requiredStringProperty("minecraftServerVersion")
// Which CommandLib module's sources to inline.
val commandlibModule: String = project.requiredStringProperty("commandlibModule")
val testPluginSuite: String = project.optionalStringProperty("testPluginSuite")
    ?: if (commandlibModule == "paper") "paper" else "spigot"
require(testPluginSuite == "spigot" || testPluginSuite == "paper") {
    "Unsupported testPluginSuite: $testPluginSuite"
}
val serverJarDownloads = project.requiredListProperty("serverJarDownloads")
val copyTargets = project.optionalListProperty("copyTargets").ifEmpty { listOf("server/plugins") }
val includeProtocolLib = project.optionalBooleanProperty("includeProtocolLib")
val autoReloaderJarDownloadUrl = providers.gradleProperty("autoReloaderJarDownloadUrl")
    .orElse(providers.environmentVariable("COMMANDLIB_AUTORELOADER_JAR_URL"))
    .orElse("https://github.com/Maru32768/AutoReloader/releases/download/1.1.0/AutoReloader-1.1.0.jar")
val defaultPluginJarDownloads = mutableListOf(
    "${resolvePlugManXDownloadUrl(minecraftServerVersion)}=>PlugManX.jar",
    "${autoReloaderJarDownloadUrl.get()}=>AutoReloader-1.1.0.jar",
)
val pluginJarDownloads = project.optionalListProperty("pluginJarDownloads").ifEmpty { defaultPluginJarDownloads }

group = "net.kunmc.lab"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    flatDir { dirs("libs") }
}

dependencies {
    platformDependencies.forEach { add("compileOnly", it) }
    add("compileOnly", "com.mojang:brigadier:1.0.18")
    if (commandlibModule == "spigot") {
        add("implementation", fileTree("../../../../spigot/build/typed-nms") { include("*.jar") })
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    sourceSets.named("main") {
        java.srcDirs(
            "../../../../$commandlibModule/src/main/java",
            "../../../../common/src/main/java",
            "../../../shared/core/src/main/java",
            "../../../shared/$testPluginSuite/src/main/java"
        )
        resources.srcDirs("../../../../$commandlibModule/src/main/resources")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    doFirst {
        copy {
            from(".")
            into(layout.buildDirectory.dir("resources/main"))
            include("LICENSE*")
        }
    }
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    relocate("net.kunmc.lab.commandlib", "$projectGroup.$projectNameLower.commandlib")
    relocate("net.kunmc.lab.configlib", "$projectGroup.$projectNameLower.configlib")
    exclude("net/minecraft")
    exclude("org/bukkit")
    exclude("com/mojang")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named("jar") {
    finalizedBy(tasks.named("shadowJar"))
}

tasks.named<Copy>("processResources") {
    val props = mapOf(
        "name" to rootProject.name,
        "version" to version,
        "MainClass" to getMainClassFQDN(projectDir.toPath())
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register("copyToServer") {
    mustRunAfter("build")
    doLast {
        copyTargets.forEach { target ->
            copy {
                from(File(layout.buildDirectory.get().asFile.absolutePath, "libs/${rootProject.name}-${version}.jar"))
                into(target)
            }
        }
    }
}

if (includeProtocolLib) {
    tasks.register<Copy>("copyProtocolLibToServer") {
        group = "copy"
        configurations["compileClasspath"].files
            .stream()
            .filter { it.name.matches(Regex(".*ProtocolLib.*\\.jar")) }
            .findFirst()
            .ifPresent { file ->
                from(file)
                into("server/plugins")
            }
    }
}

tasks.register("buildAndCopy") {
    group = "build"
    dependsOn("build", "copyToServer", "downloadPluginJars")
}

tasks.register("downloadServerJar") {
    doLast {
        serverJarDownloads.forEach { download ->
            val (urlString, relativePath) = parseDownloadEntry(download, "serverJarDownloads")
            val outputFile = projectDir.toPath().resolve(relativePath).toFile()
            downloadIfMissing(urlString, outputFile)
        }
    }
}

tasks.register("downloadPluginJars") {
    group = "setup"
    doLast {
        pluginJarDownloads.forEach { download ->
            val (urlString, fileName) = parseDownloadEntry(download, "pluginJarDownloads")
            require(!fileName.contains("/") && !fileName.contains("\\")) {
                "pluginJarDownloads destination must be a file name, not a path: $download"
            }
            copyTargets.forEach { target ->
                downloadIfMissing(urlString, projectDir.toPath().resolve(target).resolve(fileName).toFile())
            }
        }
    }
}

fun resolvePlugManXDownloadUrl(minecraftServerVersion: String): String =
    when (minecraftServerVersion) {
        "1.20.4", "1.20.5", "1.20.6", "1.21" ->
            "https://cdn.modrinth.com/data/yro4niHu/versions/bYRNAPF0/PlugManX-3.0.2.jar"

        else ->
            "https://edge.forgecdn.net/files/4529/697/PlugManX.jar"
    }

fun parseDownloadEntry(download: String, propertyName: String): Pair<String, String> =
    download.split("=>", limit = 2).let { parts ->
        require(parts.size == 2) { "Invalid $propertyName entry: $download" }
        parts[0].trim() to parts[1].trim()
    }

fun downloadIfMissing(urlString: String, outputFile: File) {
    if (outputFile.exists()) {
        return
    }

    outputFile.parentFile.mkdirs()
    URL(urlString).openStream().use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun getMainClassFQDN(projectPath: java.nio.file.Path): String {
    val mainClassFile = Files.walk(projectPath)
        .filter { it.fileName.toString().endsWith(".java") }
        .filter { file -> Files.lines(file).use { lines -> lines.anyMatch { it.contains("extends JavaPlugin") } } }
        .findFirst()
        .get()
    return mainClassFile.toString()
        .replace("\\", ".")
        .replace("/", ".")
        .replace(Regex(".*src.main.java.|.java$"), "")
}
