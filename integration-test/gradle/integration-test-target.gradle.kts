import commandlib.nms.registerBuildTools
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class CommandLibIntegrationSlot : BuildService<BuildServiceParameters.None>

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/") {
        name = "opencollab-main"
    }
    maven("https://repo.opencollab.dev/maven-releases/") {
        name = "opencollab-releases"
    }
    maven("https://repo.opencollab.dev/maven-snapshots/") {
        name = "opencollab-snapshots"
    }
    maven("https://repo.codemc.io/repository/maven-public/") {
        name = "codemc-public"
    }
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-snapshots"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-legacy-snapshots"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

// Targets are named like the modded nodes (<loader>-<minecraft version>), so a distinct group keeps Gradle from
// treating :modded:<node> and the target as the same module.
group = "dev.marulabs.integration.target"

val targetName = project.name
val targetPlatform = extra["commandlib.integration.platform"].toString()
val minecraftVersion = extra["commandlib.integration.minecraftVersion"].toString()
val minecraftJavaVersion = extra["commandlib.integration.javaVersion"].toString().toInt()
val serverDirectory = if (extra.has("commandlib.integration.serverDirectory")) {
    extra["commandlib.integration.serverDirectory"].toString()
} else {
    "server"
}
val serverJarName = if (extra.has("commandlib.integration.serverJarName")) {
    extra["commandlib.integration.serverJarName"].toString()
} else {
    "server.jar"
}
val bootstrapServerPort = if (extra.has("commandlib.integration.bootstrapServerPort")) {
    extra["commandlib.integration.bootstrapServerPort"].toString().toInt()
} else {
    25565
}
val requiresMohistBootstrap = if (extra.has("commandlib.integration.requiresMohistBootstrap")) {
    extra["commandlib.integration.requiresMohistBootstrap"].toString()
} else {
    "false"
}
    .toBooleanStrictOrNull()
    ?: false
// Arguments that make Mohist install its libraries and exit. When set, the bootstrap installs with them first and then
// starts the server without the library check, as the Docker run does.
val mohistInstallArgs = if (extra.has("commandlib.integration.mohistInstallArgs")) {
    extra["commandlib.integration.mohistInstallArgs"].toString().split(" ")
} else {
    null
}
// Directory under the target project that holds the server directory. Bukkit-family targets use their nested
// test-plugin Gradle project; other platforms only need a work directory.
val workDirectory = if (extra.has("commandlib.integration.workDirectory")) {
    extra["commandlib.integration.workDirectory"].toString()
} else {
    "test-plugin"
}
// Directory inside the server directory that receives the test plugin or mod jar.
val testPluginSubdirectory = if (extra.has("commandlib.integration.testPluginSubdirectory")) {
    extra["commandlib.integration.testPluginSubdirectory"].toString()
} else {
    "plugins"
}
val testPluginJarPattern = if (extra.has("commandlib.integration.testPluginJarPattern")) {
    extra["commandlib.integration.testPluginJarPattern"].toString()
} else {
    "TestPlugin.*\\.jar"
}
// Java arguments that start the server, such as "-jar server.jar" or an @argument file.
val serverLaunchArgs = if (extra.has("commandlib.integration.serverLaunchArgs")) {
    extra["commandlib.integration.serverLaunchArgs"].toString()
} else {
    "-jar $serverJarName"
}
val isBukkitPlatform = targetPlatform == "paper" || targetPlatform == "mohist" || targetPlatform == "spigot"
val reportFileName = "TEST-commandlib-$minecraftVersion-$targetPlatform.xml"

val mcProtocol = configurations.named("mcProtocol")
val testRuntimeClasspath = configurations.named("testRuntimeClasspath")
// The reflective runner is Java 17 bytecode for every protocol. Only its runtime dependencies and JVM vary.
evaluationDependsOn(":integration-test")
val sharedTestOutput = project(":integration-test").extensions.getByType<SourceSetContainer>()["test"].output
val minecraftIntegrationMaxParallel = providers.gradleProperty("commandlib.minecraftIntegrationMaxParallel")
    .map(String::toInt)
    .orElse(4)
val mohistBootstrapMaxParallel = providers.gradleProperty("commandlib.mohistBootstrapMaxParallel")
    .map(String::toInt)
    .orElse(4)
val containerCpuLimit = providers.gradleProperty("commandlib.containerCpuLimit")
    .map(String::toDouble)
    .orElse(2.0)
val minecraftIntegrationSlot = gradle.sharedServices.registerIfAbsent(
    "commandlibMinecraftIntegrationSlot",
    CommandLibIntegrationSlot::class,
) {
    maxParallelUsages.set(minecraftIntegrationMaxParallel)
}
val mohistBootstrapSlot = gradle.sharedServices.registerIfAbsent(
    "commandlibMohistBootstrapSlot",
    CommandLibIntegrationSlot::class,
) {
    maxParallelUsages.set(mohistBootstrapMaxParallel)
}

dependencies {
    "testImplementation"(platform("org.junit:junit-bom:5.10.2"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    "testImplementation"("org.assertj:assertj-core:3.25.3")
    "testImplementation"("org.awaitility:awaitility:4.2.1")
    "testImplementation"("org.testcontainers:junit-jupiter:1.21.4")
    "testImplementation"("org.testcontainers:testcontainers:1.21.4")
    "testRuntimeOnly"("org.slf4j:slf4j-simple:2.0.17")
    "testImplementation"("net.kyori:adventure-text-serializer-gson:4.15.0")
    "testImplementation"("net.kyori:adventure-text-serializer-json-legacy-impl:4.15.0")
}

// Java release of the test JVM, which runs the MCProtocolLib bot. MCProtocolLib needs Java 21 from 26.2.
val botJavaVersion = if (extra.has("commandlib.integration.botJavaVersion")) {
    extra["commandlib.integration.botJavaVersion"].toString().toInt()
} else {
    17
}

// Test class that drives the target. A target whose servers the shared runner cannot start, such as a proxy with a
// backend, sets its own.
val integrationTestClass = if (extra.has("commandlib.integration.testClass")) {
    extra["commandlib.integration.testClass"].toString()
} else {
    "dev.marulabs.commandlib.integration.IntegrationTest"
}

// Set by a target whose plugin the server must refuse: the text its enable failure has to log. Such a target starts
// no bot.
val expectedEnableFailure = if (extra.has("commandlib.integration.expectedEnableFailure")) {
    extra["commandlib.integration.expectedEnableFailure"].toString()
} else {
    null
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(botJavaVersion))
}

tasks.named<Test>("test") {
    enabled = false
}

// The nested test-plugin builds share this build's Gradle user home by default, so a fresh checkout or worktree
// reuses the Gradle distribution, dependencies and toolchains that are already downloaded.
val nestedGradleUserHome = providers.gradleProperty("commandlib.integrationGradleUserHome")
    .map { rootProject.file(it) }
    .getOrElse(gradle.gradleUserHomeDir)
val integrationTestDir = rootProject.file("integration-test")
val sharedDir = integrationTestDir.resolve("shared")
val testPluginDir = project.file(workDirectory)
val stagedDockerServerDir = layout.buildDirectory.dir("docker-server")
// Image reference tagged with a hash of the staged server directory, written when the directory is staged. Checkouts
// and worktrees with the same server base share the image, and one with a different base cannot replace it.
val dockerImageTagFile = layout.buildDirectory.file("docker-server-image/tag.txt")
val dockerImageRepository = "commandlib-it-${targetName.lowercase()}"

fun dockerImageExists(image: String): Boolean =
    providers.exec {
        commandLine("docker", "image", "inspect", image)
        isIgnoreExitValue = true
    }.result.get().exitValue == 0

fun hashDirectory(dir: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(1 shl 16)
    dir.walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(dir).invariantSeparatorsPath to it }
        .sortedBy { it.first }
        .forEach { (path, file) ->
            digest.update(path.toByteArray())
            digest.update(0)
            file.inputStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
        }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val javaToolchains = extensions.getByType<JavaToolchainService>()

// CommandLib module that the nested test-plugin build inlines. That build also runs on its own, so it keeps the
// setting and this build reads it from there.
val fixtureBuildScript = testPluginDir.resolve("build.gradle.kts")
    .takeIf { it.isFile }
    ?.readText()
val commandlibModule = fixtureBuildScript
    ?.let { Regex("""extra\["commandlibModule"]\s*=\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }

// A target can register its own prepareTestPlugin task before applying this script, for example to install a
// Forge server and copy the test mod built in this Gradle build. Otherwise the nested test-plugin build runs.
val prepareTask = if ("prepareTestPlugin" in tasks.names) tasks.named("prepareTestPlugin") else tasks.register<Exec>("prepareTestPlugin") {
    requireNotNull(commandlibModule) { "${testPluginDir.resolve("build.gradle.kts")} does not set commandlibModule" }
    if (commandlibModule == "spigot") {
        // The nested build bundles the typed NMS jars from spigot/build/typed-nms.
        dependsOn(":spigot:collectTypedNms")
        inputs.files(rootProject.fileTree("spigot/build/typed-nms") { include("*.jar") })
    }
    inputs.files(fileTree(testPluginDir) {
        include("*.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "gradle/**", "src/**", "libs/**", "LICENSE*")
    })
    inputs.file(sharedDir.resolve("integration-test.gradle.kts"))
    val fixtureSuite = fixtureBuildScript
        ?.let { Regex("""extra\["testPluginSuite"]\s*=\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }
        ?: if (commandlibModule == "paper") "paper" else "spigot"
    for (source in listOf("common/src/main", "$commandlibModule/src/main", "integration-test/shared/core/src/main",
        "integration-test/shared/$fixtureSuite/src/main")) {
        inputs.files(rootProject.fileTree(source))
    }
    if (fixtureSuite != "folia") inputs.files(rootProject.fileTree("integration-test/shared/bukkit/src/main"))
    if (commandlibModule == "paper") inputs.files(rootProject.fileTree("common/src/adventure"))
    if (commandlibModule == "spigot" || commandlibModule == "paper") inputs.files(rootProject.fileTree("common/src/bukkit"))
    outputs.file(testPluginDir.resolve("$serverDirectory/$serverJarName"))
    outputs.files(fileTree(testPluginDir.resolve("$serverDirectory/$testPluginSubdirectory")) {
        include("TestPlugin*.jar")
    })
    // Dependency refresh must reach the nested build even when sources have not changed.
    outputs.upToDateWhen { !gradle.startParameter.isRefreshDependencies }
    val hasWrapperJar = testPluginDir.resolve("gradle/wrapper/gradle-wrapper.jar").isFile

    workingDir(if (hasWrapperJar) testPluginDir else rootProject.projectDir)
    environment("GRADLE_USER_HOME", nestedGradleUserHome.absolutePath)

    if (isWindows) {
        if (hasWrapperJar) {
            commandLine("cmd", "/c", ".\\gradlew.bat", "prepareIntegrationFixture")
        } else {
            commandLine(
                "cmd",
                "/c",
                ".\\gradlew.bat",
                "-p",
                testPluginDir.absolutePath,
                "prepareIntegrationFixture",
            )
        }
    } else {
        if (hasWrapperJar) {
            commandLine("sh", "./gradlew", "prepareIntegrationFixture")
        } else {
            commandLine(
                "sh",
                "./gradlew",
                "-p",
                testPluginDir.absolutePath,
                "prepareIntegrationFixture",
            )
        }
    }
    if (gradle.startParameter.isOffline) args("--offline")
    if (gradle.startParameter.isRefreshDependencies) args("--refresh-dependencies")
}

// Spigot server jars are only produced by BuildTools, so a Spigot target builds its own. When a typed NMS module of the
// same version exists, the run also installs the remapped artifacts it compiles against; a target without one, such as
// spigot-1.21, needs only the server jar.
// A target for a release that BuildTools now resolves to a later build, such as 1.21, sets the Spigot build number in
// commandlib.integration.buildToolsRevision.
if (targetPlatform == "spigot") {
    val spigotVersion = minecraftVersion
    val buildToolsRevision = if (extra.has("commandlib.integration.buildToolsRevision")) {
        extra["commandlib.integration.buildToolsRevision"].toString()
    } else {
        spigotVersion
    }
    val buildSpigotServer = registerBuildTools("buildSpigotServer", provider { spigotVersion }) {
        revision.set(buildToolsRevision)
        // Spigot 1.16.5 and 26.1+ have no remapped artifacts: their modules compile against the server jar as it is.
        remapped.set(
            spigotVersion.startsWith("1.") && !spigotVersion.startsWith("1.16")
                && rootProject.file("nms/spigot-$spigotVersion").isDirectory
        )
        serverJar.set(testPluginDir.resolve("$serverDirectory/$serverJarName"))
    }
    prepareTask.configure {
        dependsOn(buildSpigotServer)
    }
}

fun writeServerProperties(serverDir: File, port: Int) {
    serverDir.mkdirs()
    // Folia 26.3 rejects the only player as "The server is full!" when max-players is 1.
    val maxPlayers = if (targetPlatform == "folia") 2 else 1
    serverDir.resolve("server.properties").writeText(
        """
        online-mode=false
        server-port=$port
        enforce-secure-profile=false
        motd=CommandLib IT
        gamemode=creative
        force-gamemode=true
        difficulty=peaceful
        max-players=$maxPlayers
        spawn-animals=false
        spawn-monsters=false
        spawn-npcs=false
        spawn-protection=0
        view-distance=2
        simulation-distance=2
        max-world-size=16
        generate-structures=false
        allow-nether=false
        allow-flight=true
        enable-command-block=true
        enable-jmx-monitoring=false
        enable-query=false
        enable-rcon=false
        enable-status=false
        network-compression-threshold=-1
        pvp=false
        rate-limit=0
        sync-chunk-writes=false
        level-type=flat
        """.trimIndent() + "\n"
    )
}

fun writeBukkitConfig(serverDir: File) {
    serverDir.resolve("bukkit.yml").writeText(
        """
        settings:
          allow-end: false
          warn-on-overload: false
          permissions-file: permissions.yml
          update-folder: update
          plugin-profiling: false
          connection-throttle: 4000
          query-plugins: false
          deprecated-verbose: false
          shutdown-message: Server closed
          minimum-api: none
          use-map-color-cache: false
        spawn-limits:
          monsters: 0
          animals: 0
          water-animals: 0
          water-ambient: 0
          water-underground-creature: 0
          ambient: 0
        chunk-gc:
          period-in-ticks: 100
        ticks-per:
          animal-spawns: 400
          monster-spawns: 400
          water-spawns: 400
          water-ambient-spawns: 400
          water-underground-creature-spawns: 400
          ambient-spawns: 400
          autosave: 0
        aliases: now-in-commands.yml
        """.trimIndent() + "\n"
    )
}

fun deleteDisabledDimensionDirectories(serverDir: File) {
    delete(
        serverDir.resolve("world/DIM1"),
        serverDir.resolve("world/DIM-1"),
    )
}

fun writeMohistConfig(serverDir: File, checkLibraries: Boolean) {
    serverDir.resolve("mohist-config").mkdirs()
    serverDir.resolve("mohist-config/mohist.yml").writeText(
        """
        mohist:
          lang: en_US
          check_update: false
          check_update_auto_download: false
          libraries:
            check: $checkLibraries
        """.trimIndent() + "\n"
    )
}

val writeDockerServerProperties = tasks.register("writeDockerServerProperties") {
    group = "setup"
    description = "Writes server.properties for the Docker-based integration test."
    dependsOn(prepareTask)

    val serverDir = testPluginDir.resolve(serverDirectory)

    doLast {
        writeServerProperties(serverDir, 25565)
        if (isBukkitPlatform) {
            writeBukkitConfig(serverDir)
        }
        deleteDisabledDimensionDirectories(serverDir)
        if (targetPlatform == "mohist") {
            writeMohistConfig(serverDir, false)
        }
    }
}

val stageDockerServerDirectory = tasks.register<Sync>("stageDockerServerDirectory") {
    group = "setup"
    description = "Stages a slim server directory before copying it into the Docker container."
    dependsOn(writeDockerServerProperties)

    val serverDir = testPluginDir.resolve(serverDirectory)

    from(serverDir)
    into(stagedDockerServerDir)
    if (isBukkitPlatform || targetPlatform == "folia") {
        // The fixture is injected at startup. Manual reload helpers and their remapping caches are not test inputs.
        exclude("plugins/**")
    }
    exclude(
        "logs/**",
        "crash-reports/**",
        // A world or a config a local run left behind would carry its state into every test: an old paper.yml's
        // keep-spawn-loaded-range keeps Paper 1.16.5 from ticking the test's command blocks. The warm-up generates
        // them.
        "world/**",
        "world_*/**",
        "paper.yml",
        "spigot.yml",
        "commands.yml",
        "help.yml",
        "permissions.yml",
        "config/**",
        "flat/**",
        "flat_*/**",
        "plugins/TestPlugin*.jar",
        "mods/TestMod*.jar",
        "usercache.json",
        "usernamecache.json",
        "banned-ips.json",
        "banned-players.json",
        "whitelist.json",
        "ops.json",
    )
    doLast {
        val stagedDir = stagedDockerServerDir.get().asFile
        // Servers download, patch and remap the server jar, download plugin libraries and generate the world on
        // their first start, depending on the platform. Starting the server once while building the image keeps
        // that work, and its network access, out of every test run; Docker caches the layer until the staged
        // directory changes.
        stagedDir.resolve("commandlib-warmup.sh").writeText(
            """
            set -e
            printf 'eula=true\n' > eula.txt
            mkfifo /tmp/console
            java -Dplugin.env=CI $serverLaunchArgs nogui < /tmp/console > /tmp/warmup.log 2>&1 &
            pid=${'$'}!
            exec 3> /tmp/console
            waited=0
            until grep -q 'Done (' /tmp/warmup.log; do
              if ! kill -0 ${'$'}pid 2> /dev/null || [ ${'$'}waited -ge 600 ]; then
                cat /tmp/warmup.log
                echo 'The server did not start while warming up the image.'
                exit 1
              fi
              sleep 1
              waited=${'$'}((waited + 1))
            done
            echo stop >&3
            waited=0
            while kill -0 ${'$'}pid 2> /dev/null && [ ${'$'}waited -lt 120 ]; do
              sleep 1
              waited=${'$'}((waited + 1))
            done
            if kill -0 ${'$'}pid 2> /dev/null; then
              kill -9 ${'$'}pid 2> /dev/null || true
              cat /tmp/warmup.log
              echo 'The server did not stop cleanly while warming up the image.'
              exit 1
            fi
            if ! wait ${'$'}pid; then
              cat /tmp/warmup.log
              echo 'The server failed while warming up the image.'
              exit 1
            fi
            rm -rf logs crash-reports world/session.lock commandlib-warmup.sh
            """.trimIndent() + "\n"
        )
        stagedDir.resolve("Dockerfile").writeText(
            """
            FROM eclipse-temurin:$minecraftJavaVersion-jre
            WORKDIR /workspace/server
            COPY . /workspace/server
            RUN sh commandlib-warmup.sh
            """.trimIndent() + "\n"
        )
        dockerImageTagFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("$dockerImageRepository:${hashDirectory(stagedDir).take(16)}\n")
        }
    }
    outputs.file(dockerImageTagFile)
}

val buildMinecraftServerImage = tasks.register<Exec>("buildMinecraftServerImage") {
    group = "verification"
    description = "Builds the reusable Docker image for the $targetName integration server."
    dependsOn(stageDockerServerDirectory)
    // A cold image build starts a real server too, so it shares the test runtime's concurrency budget.
    usesService(minecraftIntegrationSlot)
    // Also rebuilds an image that was removed, for example by `docker image prune -a`.
    onlyIf("the image for the staged server is not in Docker yet") {
        !dockerImageExists(dockerImageTagFile.get().asFile.readText().trim())
    }

    doFirst {
        commandLine(
            "docker",
            "build",
            "--pull=false",
            "-t",
            dockerImageTagFile.get().asFile.readText().trim(),
            stagedDockerServerDir.get().asFile.absolutePath,
        )
    }
}

val writeBootstrapServerProperties = tasks.register("writeBootstrapServerProperties") {
    group = "setup"
    description = "Writes server.properties for the host-side bootstrap server."
    dependsOn(prepareTask)

    val serverDir = testPluginDir.resolve(serverDirectory)

    onlyIf {
        requiresMohistBootstrap
    }

    doLast {
        writeServerProperties(serverDir, bootstrapServerPort)
        writeBukkitConfig(serverDir)
        deleteDisabledDimensionDirectories(serverDir)
        writeMohistConfig(serverDir, true)
    }
}

val mohistBootstrapTask = tasks.register("bootstrapMohist") {
    group = "verification"
    description = "Runs $targetName once when Mohist has not generated its runtime files yet."
    dependsOn(prepareTask, writeBootstrapServerProperties)
    usesService(mohistBootstrapSlot)

    val serverDir = testPluginDir.resolve(serverDirectory)
    val serverJar = serverDir.resolve(serverJarName)
    val requiredPaths = listOf(
        serverDir.resolve("libraries"),
        serverDir.resolve("world"),
    )
    val javaExecutable = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(minecraftJavaVersion))
    }.map { it.executablePath.asFile }

    onlyIf {
        requiresMohistBootstrap && requiredPaths.any { !it.exists() }
    }

    doLast {
        if (!serverJar.isFile) {
            throw GradleException("Mohist server jar does not exist: ${serverJar.absolutePath}")
        }

        serverDir.mkdirs()
        serverDir.resolve("eula.txt").writeText("eula=true\n")

        if (mohistInstallArgs != null) {
            val install = ProcessBuilder(javaExecutable.get().absolutePath, "-jar", serverJar.name, *mohistInstallArgs.toTypedArray())
                .directory(serverDir)
                .redirectErrorStream(true)
                .start()
            val installOutput = StringBuilder()
            val installReader = Thread {
                install.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        installOutput.appendLine(line)
                        logger.lifecycle("[$targetName] $line")
                    }
                }
            }
            installReader.isDaemon = true
            installReader.start()
            if (!install.waitFor(10, TimeUnit.MINUTES)) {
                install.destroyForcibly()
                throw GradleException("Timed out while installing $targetName. Last output:\n$installOutput")
            }
            installReader.join(TimeUnit.SECONDS.toMillis(10))
            if (install.exitValue() != 0) {
                throw GradleException("Installing $targetName failed. Output:\n$installOutput")
            }
            writeMohistConfig(serverDir, false)
        }

        val process = ProcessBuilder(
            javaExecutable.get().absolutePath,
            "-jar",
            serverJar.name,
            "nogui",
        )
            .directory(serverDir)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        val ready = AtomicBoolean(false)
        val reader = process.inputStream.bufferedReader()
        val outputThread = Thread {
            reader.useLines { lines ->
                lines.forEach { line ->
                    output.appendLine(line)
                    logger.lifecycle("[$targetName] $line")
                    if (line.contains("Done (") || line.contains("Done(")) {
                        ready.set(true)
                    }
                }
            }
        }
        outputThread.isDaemon = true
        outputThread.start()

        val started = System.nanoTime()
        val timeoutNanos = TimeUnit.MINUTES.toNanos(10)
        while (process.isAlive && !ready.get() && System.nanoTime() - started < timeoutNanos) {
            Thread.sleep(500)
        }

        if (!ready.get()) {
            process.destroy()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            throw GradleException("Timed out while bootstrapping $targetName. Last output:\n$output")
        }

        process.outputStream.bufferedWriter().use {
            it.write("stop")
            it.newLine()
            it.flush()
        }

        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw GradleException("Mohist bootstrap did not stop cleanly for $targetName.")
        }
        outputThread.join(TimeUnit.SECONDS.toMillis(10))

        val missingPaths = requiredPaths.filter { !it.exists() }
        if (missingPaths.isNotEmpty()) {
            throw GradleException(
                "Mohist bootstrap finished but required files are still missing: "
                        + missingPaths.joinToString { it.absolutePath }
            )
        }
    }
}

if (requiresMohistBootstrap) {
    buildMinecraftServerImage.configure {
        dependsOn(mohistBootstrapTask)
    }
    writeDockerServerProperties.configure {
        mustRunAfter(mohistBootstrapTask)
    }
}

tasks.register<Test>("minecraftIntegrationTest") {
    group = "verification"
    description = "Runs the Docker-based Minecraft integration test for $minecraftVersion."
    usesService(minecraftIntegrationSlot)
    outputs.upToDateWhen { false }
    testClassesDirs = sharedTestOutput.classesDirs
    dependsOn(":integration-test:testClasses")
    // The test reads Adventure only by reflection. Its own Adventure 4 goes first for bots that bring none or an older
    // one, and MCProtocolLib first for bots built on Adventure 5 (26.2 and later), which Adventure 4 would shadow.
    classpath = files(provider {
        val botUsesAdventure5 = mcProtocol.get().incoming.resolutionResult.allComponents.any {
            val module = it.moduleVersion
            module != null && module.group == "net.kyori" && module.name == "adventure-api" && module.version.startsWith("5.")
        }
        if (botUsesAdventure5) listOf(mcProtocol, sharedTestOutput, testRuntimeClasspath)
        else listOf(sharedTestOutput, testRuntimeClasspath, mcProtocol)
    })
    useJUnitPlatform()
    filter {
        includeTestsMatching(integrationTestClass)
    }
    testLogging {
        events("started", "passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    dependsOn(buildMinecraftServerImage)
    if (requiresMohistBootstrap) {
        dependsOn(mohistBootstrapTask)
    }
    systemProperty("commandlib.rootDir", rootProject.projectDir.absolutePath)
    systemProperty("commandlib.integrationTestDir", integrationTestDir.absolutePath)
    systemProperty("commandlib.testPluginDir", testPluginDir.absolutePath)
    doFirst {
        systemProperty("commandlib.dockerImageName", dockerImageTagFile.get().asFile.readText().trim())
    }
    systemProperty("commandlib.sharedDir", sharedDir.absolutePath)
    systemProperty("commandlib.runMinecraftIntegration", "true")
    systemProperty("commandlib.targetName", targetName)
    expectedEnableFailure?.let { systemProperty("commandlib.expectedEnableFailure", it) }
    systemProperty("commandlib.platform", targetPlatform)
    systemProperty("commandlib.reportFileName", reportFileName)
    systemProperty("commandlib.serverDirectory", serverDirectory)
    systemProperty("commandlib.serverJarName", serverJarName)
    systemProperty("commandlib.serverLaunchArgs", serverLaunchArgs)
    systemProperty("commandlib.testPluginSubdirectory", testPluginSubdirectory)
    systemProperty("commandlib.testPluginJarPattern", testPluginJarPattern)
    systemProperty("commandlib.minecraftJavaVersion", minecraftJavaVersion.toString())
    systemProperty("commandlib.containerCpuLimit", containerCpuLimit.get().toString())
}
