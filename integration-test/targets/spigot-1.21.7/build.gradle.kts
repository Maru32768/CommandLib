val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.21.7"
extra["cadence.integration.javaVersion"] = "21"
// BuildTools now resolves 1.21.7 to the 1.21.8 build, so build the last 1.21.7 build.
extra["cadence.integration.buildToolsRevision"] = "4522"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.7-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
