rootProject.name = "SamplePaperPlugin"

val commandLibRoot = file("../..")
if (commandLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(commandLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.CommandLib:paper"))
                .using(project(":paper"))
            substitute(module("com.github.Maru32768.CommandLib:paper-testing"))
                .using(project(":paper-testing"))
        }
    }
}
