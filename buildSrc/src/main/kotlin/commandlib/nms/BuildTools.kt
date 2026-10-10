package commandlib.nms

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File

private const val BUILD_TOOLS_URL =
    "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"

/**
 * Registers a [BuildToolsTask] for the Minecraft version, sharing the BuildTools directory, the local Maven repository
 * and the service that serializes runs with every other BuildTools task of the build. The typed NMS modules use it to
 * install `org.spigotmc:spigot`, and the Spigot integration targets to build the server jar.
 */
fun Project.registerBuildTools(
    name: String,
    minecraftVersion: Provider<String>,
    configure: BuildToolsTask.() -> Unit = {},
): TaskProvider<BuildToolsTask> {
    val service = gradle.sharedServices.registerIfAbsent("commandlibBuildTools", BuildToolsService::class.java) {
        maxParallelUsages.set(1)
    }
    val javaToolchains = extensions.getByType<JavaToolchainService>()
    return tasks.register<BuildToolsTask>(name) {
        usesService(service)
        this.minecraftVersion.set(minecraftVersion)
        revision.convention(minecraftVersion)
        remapped.convention(false)
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(minecraftVersion.map { JavaLanguageVersion.of(buildToolsJavaVersion(it)) })
        })
        buildToolsUrl.set(providers.gradleProperty("commandlib.buildToolsUrl").orElse(BUILD_TOOLS_URL))
        buildToolsDirectory.set(File(gradle.gradleUserHomeDir, "caches/commandlib-buildtools"))
        mavenLocalDirectory.set(mavenLocalRepository())
        configure()
    }
}
