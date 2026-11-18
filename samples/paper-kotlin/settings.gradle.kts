rootProject.name = "SamplePaperKotlinPlugin"

val cadenceRoot = file("../..")
if (cadenceRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(cadenceRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.Cadence:paper-kotlin"))
                .using(project(":paper-kotlin"))
        }
    }
}
