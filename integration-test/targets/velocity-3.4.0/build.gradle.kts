/*
 * A smoke target for Velocity: the velocity artifact in a plugin on a real Velocity 3.4.0 proxy, with a Paper 1.21.4
 * backend, and the bot connecting through the proxy. VelocityIntegrationTest runs it.
 *
 * The image holds both servers: the backend in the server directory, which the image build warms up like any Paper
 * target, and the proxy in its proxy subdirectory. The test starts one container of each on a shared network.
 */

import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "velocity"
extra["commandlib.integration.minecraftVersion"] = "3.4.0"
// Paper 1.21.4 needs Java 21; Velocity 3.4 runs on it too.
extra["commandlib.integration.javaVersion"] = "21"
extra["commandlib.integration.workDirectory"] = "work"
extra["commandlib.integration.testPluginSubdirectory"] = "proxy/plugins"
extra["commandlib.integration.testClass"] = "dev.marulabs.commandlib.integration.VelocityIntegrationTest"

val velocityJarUrl =
    "https://fill-data.papermc.io/v1/objects/fb599cbda6a6d01decce5e281f71f51cae7cacffcfafca32a09601f407b0583e/velocity-3.4.0-566.jar"
val backendJarUrl =
    "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

val testPlugin by sourceSets.creating

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.4-SNAPSHOT")
    "testPluginCompileOnly"("com.velocitypowered:velocity-api:3.4.0")
    // Writes velocity-plugin.json from @Plugin, as a downstream plugin build does.
    "testPluginAnnotationProcessor"("com.velocitypowered:velocity-api:3.4.0")
    "testPluginImplementation"(project(":velocity"))
}

// The plugin with CommandLib (velocity and common) inside it, as a downstream plugin shades it.
val testPluginJar = tasks.register<Jar>("testPluginJar") {
    archiveFileName.set("TestPlugin-velocity.jar")
    destinationDirectory.set(layout.buildDirectory.dir("test-plugin"))
    from(testPlugin.output)
    from(testPlugin.runtimeClasspath.filter { it.name.endsWith(".jar") }.elements.map { jars -> jars.map { zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val serverDir = file("work/server")

fun download(url: String, target: File) {
    if (target.isFile) {
        return
    }
    target.parentFile.mkdirs()
    val part = File(target.parentFile, target.name + ".part")
    URI(url).toURL().openStream().use { Files.copy(it, part.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    Files.move(part.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
}

tasks.register("prepareTestPlugin") {
    group = "setup"
    description = "Downloads the Paper backend and the Velocity proxy and copies the CommandLib test plugin to the proxy."
    dependsOn(testPluginJar)
    doLast {
        download(backendJarUrl, serverDir.resolve("server.jar"))
        download(velocityJarUrl, serverDir.resolve("proxy/velocity.jar"))
        copy {
            from("velocity.toml")
            into(serverDir.resolve("proxy"))
        }
        sync {
            from(testPluginJar)
            into(serverDir.resolve("proxy/plugins"))
        }
    }
}

apply(from = "../../gradle/integration-test-target.gradle.kts")

// The test plugin is copied into the container at test startup, so a change to it does not rebuild the image.
tasks.named<Sync>("stageDockerServerDirectory") {
    exclude("proxy/plugins/**", "proxy/logs/**", "proxy/forwarding.secret")
}
