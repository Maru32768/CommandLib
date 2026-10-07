package commandlib.nms

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.api.tasks.bundling.Jar
import java.io.File

/**
 * Compiles a typed NMS module against `org.spigotmc:spigot`, which only BuildTools produces. `installSpigot` runs
 * BuildTools when the local Maven repository lacks the artifacts, so `compileJava` works on a clean machine.
 *
 * The module exposes the jar to bundle into `:spigot` through the `nmsElements` configuration: the plain jar, or with
 * `remapped` the jar remapped from Mojang names to Spigot names in two steps (Mojang to obfuscated, obfuscated to
 * Spigot), like CommandAPI's Spigot NMS modules.
 */
class SpigotNmsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        val extension = project.extensions.create<SpigotNmsExtension>("spigotNms")
        extension.remapped.convention(false)
        val spigotVersion = extension.minecraftVersion.map { "$it-R0.1-SNAPSHOT" }

        val mavenLocalDirectory = System.getProperty("maven.repo.local")
            ?.let(::File)
            ?: File(System.getProperty("user.home"), ".m2/repository")
        project.repositories.mavenLocal {
            content { includeGroup("org.spigotmc") }
            metadataSources {
                mavenPom()
                artifact()
            }
        }
        // Repositories for the dependencies declared by the Spigot server POM.
        project.repositories.maven { url = project.uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        project.repositories.maven { url = project.uri("https://libraries.minecraft.net") }
        project.repositories.mavenCentral()

        val buildToolsService = project.gradle.sharedServices.registerIfAbsent(
            "commandlibBuildTools",
            BuildToolsService::class.java,
        ) {
            maxParallelUsages.set(1)
        }
        val javaToolchains = project.extensions.getByType<JavaToolchainService>()
        val installSpigot = project.tasks.register<BuildToolsTask>("installSpigot") {
            usesService(buildToolsService)
            minecraftVersion.set(extension.minecraftVersion)
            remapped.set(extension.remapped)
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(extension.minecraftVersion.map { JavaLanguageVersion.of(buildToolsJavaVersion(it)) })
            })
            buildToolsUrl.set(project.providers.gradleProperty("commandlib.buildToolsUrl").orElse(BUILD_TOOLS_URL))
            buildToolsDirectory.set(File(project.gradle.gradleUserHomeDir, "caches/commandlib-buildtools"))
            this.mavenLocalDirectory.set(mavenLocalDirectory)
        }
        project.tasks.named("compileJava") {
            dependsOn(installSpigot)
        }

        project.dependencies.addProvider(
            "compileOnly",
            spigotVersion.zip(extension.remapped) { version, remapped ->
                "org.spigotmc:spigot:$version" + if (remapped) ":remapped-mojang" else ""
            },
        )

        project.configurations.create("nmsElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
        }

        project.afterEvaluate {
            val jar = tasks.named<Jar>("jar")
            if (!extension.remapped.get()) {
                artifacts.add("nmsElements", jar)
                return@afterEvaluate
            }

            val remapMojangToObf = tasks.register<SpigotRemapTask>("remapMojangToObf") {
                inputJar.set(jar.flatMap { it.archiveFile })
                outputJar.set(layout.buildDirectory.file("remap/${project.name}-obf.jar"))
                mappingFile.set(mappingFile(spigotVersion, "maps-mojang@txt"))
                inheritanceJars.from(configurations.named("compileClasspath"))
                reverse.set(true)
            }
            val remapObfToSpigot = tasks.register<SpigotRemapTask>("remapObfToSpigot") {
                inputJar.set(remapMojangToObf.flatMap { it.outputJar })
                outputJar.set(layout.buildDirectory.file("remap/${project.name}-spigot.jar"))
                mappingFile.set(mappingFile(spigotVersion, "maps-spigot@csrg"))
                inheritanceJars.from(configurations.named("compileClasspath"))
                reverse.set(false)
            }
            artifacts.add("nmsElements", remapObfToSpigot.flatMap { it.outputJar })
            tasks.named("assemble") {
                dependsOn(remapObfToSpigot)
            }
        }
    }

    private fun Project.mappingFile(spigotVersion: Provider<String>, classifierAndExtension: String): Provider<RegularFile> =
        layout.file(spigotVersion.map { version ->
            configurations.detachedConfiguration(
                dependencies.create("org.spigotmc:minecraft-server:$version:$classifierAndExtension")
            ).singleFile
        })

    private companion object {
        const val BUILD_TOOLS_URL =
            "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
    }
}
