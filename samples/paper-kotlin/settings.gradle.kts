rootProject.name = "SamplePaperKotlinPlugin"

val commandLibRoot = file("../..")
if (commandLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(commandLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.CommandLib:paper-kotlin"))
                .using(project(":paper-kotlin"))
        }
    }
}
