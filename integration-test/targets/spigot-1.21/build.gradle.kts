val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.21"
extra["commandlib.integration.javaVersion"] = "21"
// BuildTools now resolves 1.21 to the build of the following hotfix release, so build the last 1.21 build.
extra["commandlib.integration.buildToolsRevision"] = "4289"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.21-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
