val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.20"
extra["commandlib.integration.javaVersion"] = "17"
// BuildTools now resolves 1.20 to the 1.20.1 build, so build the last 1.20 build.
extra["commandlib.integration.buildToolsRevision"] = "3798"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
