/*
 * Prepares a Forge integration-test target. Apply it before integration-test-target.gradle.kts:
 *
 *   extra["commandlib.integration.forgeVersion"] = "1.20.1-47.4.10"
 *   apply(from = "../../gradle/forge-integration-target.gradle.kts")
 *   apply(from = "../../gradle/integration-test-target.gradle.kts")
 *
 * It registers prepareTestPlugin, which installs the Forge server with the official installer and copies
 * the test mod built by :integration-test:shared:forge-fixture:<minecraft version> into the mods directory.
 * The shared target script then uses that task instead of building a nested test-plugin project.
 */

import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val forgeVersion = extra["commandlib.integration.forgeVersion"].toString()
val forgeMinecraftVersion = extra["commandlib.integration.minecraftVersion"].toString()
val forgeJavaVersion = extra["commandlib.integration.javaVersion"].toString().toInt()
val forgeWorkDir = project.file(extra["commandlib.integration.workDirectory"].toString())
val forgeServerDir = forgeWorkDir.resolve("server")
// The last launch argument names the server jar (-jar <jar>) or the argument file (@<file>).
val forgeLaunchFile = extra["commandlib.integration.serverLaunchArgs"].toString()
    .trim()
    .split(Regex("\\s+"))
    .last()
    .removePrefix("@")
val forgeInstaller =layout.buildDirectory.file("forge/forge-$forgeVersion-installer.jar")
val forgeJavaLauncher = extensions.getByType<JavaToolchainService>()
    .launcherFor { languageVersion.set(JavaLanguageVersion.of(forgeJavaVersion)) }

val fixturePath = ":integration-test:shared:forge-fixture:$forgeMinecraftVersion"
evaluationDependsOn(fixturePath)
val testModJar = project(fixturePath).tasks.named<Jar>("testModJar")

val downloadForgeInstaller = tasks.register("downloadForgeInstaller") {
    group = "setup"
    description = "Downloads the Forge $forgeVersion installer."
    val installer = forgeInstaller
    outputs.file(installer)

    doLast {
        val target = installer.get().asFile.toPath()
        Files.createDirectories(target.parent)
        val url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$forgeVersion/forge-$forgeVersion-installer.jar"
        URI(url).toURL()
            .openStream()
            .use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
    }
}

val installForgeServer = tasks.register<Exec>("installForgeServer") {
    group = "setup"
    description = "Installs the Forge $forgeVersion server into ${forgeServerDir.relativeTo(projectDir)}."
    dependsOn(downloadForgeInstaller)
    // The installer creates the libraries directory before running its processors, so an interrupted
    // install would leave it behind. Check the file the server is launched from instead, which is written last.
    val installedMarker = forgeServerDir.resolve(forgeLaunchFile)
    onlyIf { !installedMarker.exists() }

    doFirst {
        forgeServerDir.mkdirs()
        executable(forgeJavaLauncher.get().executablePath.asFile.absolutePath)
    }
    workingDir(forgeServerDir)
    args("-jar", forgeInstaller.get().asFile.absolutePath, "--installServer", forgeServerDir.absolutePath)
}

tasks.register<Sync>("prepareTestPlugin") {
    group = "setup"
    description = "Installs the Forge server and copies the CommandLib test mod into its mods directory."
    dependsOn(installForgeServer)
    from(testModJar)
    into(forgeServerDir.resolve("mods"))
}
