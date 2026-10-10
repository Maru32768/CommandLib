package commandlib.nms

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
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
import java.io.RandomAccessFile
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import javax.inject.Inject

/**
 * Serializes the BuildTools tasks of one build, because they share one working directory. [BuildToolsTask] also locks
 * the directory against other Gradle processes.
 */
abstract class BuildToolsService : BuildService<BuildServiceParameters.None>

/**
 * Installs `org.spigotmc:spigot` for one Minecraft version into the local Maven repository by running
 * [BuildTools](https://www.spigotmc.org/wiki/buildtools/), and copies the runnable server jar to [serverJar] when it is
 * set. Nothing runs when the artifacts of the revision are installed already, so only the first build of a version
 * pays for BuildTools.
 */
@DisableCachingByDefault(because = "Installs into the local Maven repository")
abstract class BuildToolsTask : DefaultTask() {
    /** Minecraft version of the artifacts BuildTools installs, such as `1.21`. */
    @get:Input
    abstract val minecraftVersion: Property<String>

    /**
     * Passed to BuildTools as `--rev`. Defaults to [minecraftVersion]. A Spigot build number selects a release whose
     * version name BuildTools now resolves to a later build, such as 1.21, which resolves to the 1.21.1 build.
     */
    @get:Input
    abstract val revision: Property<String>

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

    /** Where to copy the runnable Spigot server jar, for the integration tests. */
    @get:Internal
    abstract val serverJar: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "setup"
        description = "Installs the Spigot server artifacts into the local Maven repository with BuildTools."
    }

    @TaskAction
    fun install() {
        val directory = buildToolsDirectory.get().asFile.apply { mkdirs() }
        // Another Gradle process, such as an IDE sync next to a command-line build, may run BuildTools in the same
        // directory. BuildTools writes the server jar in place as its last step, so checking the artifacts and copying
        // the jar hold the lock too. The lock is released when the channel closes, and by the OS when a process dies.
        RandomAccessFile(directory.resolve("buildtools.lock"), "rw").channel.use { channel ->
            val lock = channel.tryLock() ?: run {
                logger.lifecycle("Waiting for another Gradle process to finish running BuildTools in $directory")
                channel.lock()
            }
            lock.use {
                var ranBuildTools = false
                if (!isInstalled()) {
                    runBuildTools(directory)
                    ranBuildTools = true
                }
                didWork = copyServerJar() || ranBuildTools
            }
        }
    }

    private fun runBuildTools(directory: File) {
        val buildToolsJar = directory.resolve("BuildTools.jar")
        updateBuildTools(buildToolsJar)

        val workDirectory = directory.resolve("work").apply { mkdirs() }
        val buildToolsArgs = mutableListOf("-jar", buildToolsJar.absolutePath, "--rev", revision.get())
        if (remapped.get()) {
            buildToolsArgs += "--remapped"
        }
        logger.lifecycle("Running BuildTools for Spigot ${minecraftVersion.get()}. This takes several minutes.")
        val repository = mavenLocalDirectory.get().asFile
        execOperations.exec {
            workingDir = workDirectory
            executable = javaLauncher.get().executablePath.asFile.absolutePath
            args(buildToolsArgs)
            // BuildTools sets -Xmx1024M only when MAVEN_OPTS is unset, so keep that default.
            val mavenOpts = System.getenv("MAVEN_OPTS") ?: "-Xmx1024M"
            environment("MAVEN_OPTS", mavenOpts + repositoryOption(repository, mavenOpts))
        }

        val stillMissing = missingArtifacts()
        if (stillMissing.isNotEmpty()) {
            error("BuildTools finished but did not install: ${stillMissing.joinToString()}")
        }
        revisionRecord().apply { parentFile.mkdirs() }.writeText(revision.get())
    }

    /**
     * Whether the artifacts are installed, built from [revision]. A revision that an earlier run recorded and that
     * differs means another build of the release, which BuildTools replaces. Artifacts installed before runs recorded
     * their revision are taken to match, and the revision is recorded for them.
     */
    private fun isInstalled(): Boolean {
        if (missingArtifacts().isNotEmpty()) {
            return false
        }
        val record = revisionRecord()
        if (!record.isFile) {
            record.parentFile.mkdirs()
            record.writeText(revision.get())
            return true
        }
        return record.readText().trim() == revision.get()
    }

    private fun revisionRecord(): File =
        buildToolsDirectory.get().asFile.resolve("revisions/spigot-${minecraftVersion.get()}.txt")

    /**
     * BuildTools installs with its own Maven, which finds the repository in `~/.m2/settings.xml` or at `~/.m2/repository`
     * but would not see a `maven.repo.local` given to this build or `$M2_HOME/conf/settings.xml`. The option goes
     * through `MAVEN_OPTS`, which the `mvn` shell script splits on whitespace, so only `mvn.cmd` can take a quoted path
     * with spaces. It is left out only for the repository Maven finds by itself, and only when the inherited
     * `MAVEN_OPTS` names no repository, because the last `-Dmaven.repo.local` wins.
     */
    private fun repositoryOption(repository: File, mavenOpts: String): String {
        if (repository.absoluteFile == userMavenRepository().absoluteFile && !mavenOpts.contains("maven.repo.local")) {
            return ""
        }
        val path = repository.absolutePath
        if (path.none(Char::isWhitespace)) {
            return " -Dmaven.repo.local=$path"
        }
        check(System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            "BuildTools cannot install into $path, because MAVEN_OPTS cannot carry a path with spaces outside Windows. " +
                "Set <localRepository> in ~/.m2/settings.xml or use a local Maven repository without spaces."
        }
        return " \"-Dmaven.repo.local=$path\""
    }

    /** BuildTools leaves the server jar in its working directory, named after the version. */
    private fun builtServerJar(): File =
        buildToolsDirectory.get().asFile.resolve("work/spigot-${minecraftVersion.get()}.jar")

    /**
     * Copies the server jar with its modification time, so a copy with the same size and time is taken to be current
     * without reading either jar.
     */
    private fun copyServerJar(): Boolean {
        val destination = serverJar.orNull?.asFile ?: return false
        val source = builtServerJar()
        if (destination.isFile && destination.length() == source.length() &&
            destination.lastModified() == source.lastModified()
        ) {
            return false
        }
        destination.parentFile.mkdirs()
        Files.copy(
            source.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES,
        )
        return true
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
        if (serverJar.isPresent) {
            required += builtServerJar()
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
