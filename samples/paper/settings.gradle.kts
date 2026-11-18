rootProject.name = "SamplePaperPlugin"

val cadenceRoot = file("../..")
if (cadenceRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(cadenceRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.Cadence:paper"))
                .using(project(":paper"))
            substitute(module("com.github.Maru32768.Cadence:paper-testing"))
                .using(project(":paper-testing"))
        }
    }
}
