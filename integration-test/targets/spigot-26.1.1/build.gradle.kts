val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "26.1.1"
extra["cadence.integration.javaVersion"] = "25"
// BuildTools now resolves 26.1.1 to the 26.1.2 build, so build the last 26.1.1 build.
extra["cadence.integration.buildToolsRevision"] = "4617"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.1-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
