package commandlib.nms

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import javax.inject.Inject

/** Serializes BuildTools runs, because they share one working directory. */
abstract class BuildToolsService : BuildService<BuildServiceParameters.None>

/**
 * Installs `org.spigotmc:spigot` for one Minecraft version into the local Maven repository by running
 * [BuildTools](https://www.spigotmc.org/wiki/buildtools/). Nothing runs when the artifacts are installed already,
 * so only the first build of a version pays for BuildTools.
 */
@DisableCachingByDefault(because = "Installs into the local Maven repository")
abstract class BuildToolsTask : DefaultTask() {
    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val remapped: Property<Boolean>

    /** Resolved only when BuildTools runs, so an installed version needs no extra JDK. */
    @get:Internal
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Input
    abstract val buildToolsUrl: Property<String>

    @get:Internal
    abstract val buildToolsDirectory: DirectoryProperty

    @get:Internal
    abstract val mavenLocalDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "setup"
        description = "Installs the Spigot server artifacts into the local Maven repository with BuildTools."
    }

    @TaskAction
    fun install() {
        if (missingArtifacts().isEmpty()) {
            didWork = false
            return
        }

        val directory = buildToolsDirectory.get().asFile
        val buildToolsJar = directory.resolve("BuildTools.jar")
        updateBuildTools(buildToolsJar)

        val workDirectory = directory.resolve("work").apply { mkdirs() }
        val buildToolsArgs = mutableListOf("-jar", buildToolsJar.absolutePath, "--rev", minecraftVersion.get())
        if (remapped.get()) {
            buildToolsArgs += "--remapped"
        }
        logger.lifecycle("Running BuildTools for Spigot ${minecraftVersion.get()}. This takes several minutes.")
        val repository = mavenLocalDirectory.get().asFile
        execOperations.exec {
            workingDir = workDirectory
            executable = javaLauncher.get().executablePath.asFile.absolutePath
            args(buildToolsArgs)
            // BuildTools installs with its own Maven, which would not see a maven.repo.local given to this build.
            // BuildTools sets -Xmx1024M only when MAVEN_OPTS is unset, so keep that default.
            val mavenOpts = System.getenv("MAVEN_OPTS") ?: "-Xmx1024M"
            environment("MAVEN_OPTS", "$mavenOpts -Dmaven.repo.local=${repository.absolutePath}")
        }

        val stillMissing = missingArtifacts()
        if (stillMissing.isNotEmpty()) {
            error("BuildTools finished but did not install: ${stillMissing.joinToString()}")
        }
    }

    private fun missingArtifacts(): List<File> {
        val version = "${minecraftVersion.get()}-R0.1-SNAPSHOT"
        val repository = mavenLocalDirectory.get().asFile
        val spigot = repository.resolve("org/spigotmc/spigot/$version")
        val required = mutableListOf(spigot.resolve("spigot-$version.jar"))
        if (remapped.get()) {
            val server = repository.resolve("org/spigotmc/minecraft-server/$version")
            required += spigot.resolve("spigot-$version-remapped-mojang.jar")
            required += server.resolve("minecraft-server-$version-maps-mojang.txt")
            required += server.resolve("minecraft-server-$version-maps-spigot.csrg")
            if (hasSpigotMemberMappings(minecraftVersion.get())) {
                required += server.resolve("minecraft-server-$version-maps-spigot-members.csrg")
            }
        }
        return required.filterNot(File::isFile)
    }

    /**
     * Downloads BuildTools before each run, because a new Minecraft version can need a newer one than the cached jar.
     * Runs are rare, and BuildTools needs network access anyway. The cached jar is kept when the download fails.
     */
    private fun updateBuildTools(destination: File) {
        destination.parentFile.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.part")
        val url = buildToolsUrl.get()
        logger.lifecycle("Downloading BuildTools from $url")
        try {
            val connection = URI(url).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 60_000
            }
            connection.getInputStream().use { Files.copy(it, temporary.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            // SpigotMC publishes no checksum for BuildTools.jar, so this only rejects a truncated download or an
            // error page. The download itself goes over HTTPS.
            val mainClass = JarFile(temporary).use { it.manifest?.mainAttributes?.getValue("Main-Class") }
            check(mainClass != null) { "$url is not an executable jar" }
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            temporary.delete()
            if (!destination.isFile) {
                throw e
            }
            logger.warn("Could not download BuildTools ({}); using the cached jar.", e.toString())
        }
    }
}
