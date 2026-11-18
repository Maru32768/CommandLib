rootProject.name = "SampleSpigotPlugin"

val cadenceRoot = file("../..")
if (cadenceRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(cadenceRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.Cadence:spigot"))
                .using(project(":spigot"))
        }
    }
}
