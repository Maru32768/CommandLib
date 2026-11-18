rootProject.name = "SampleVelocityPlugin"

val cadenceRoot = file("../..")
if (cadenceRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(cadenceRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.Cadence:velocity"))
                .using(project(":velocity"))
        }
    }
}
