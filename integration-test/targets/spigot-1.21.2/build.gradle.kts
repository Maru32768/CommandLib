val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.21.2"
extra["cadence.integration.javaVersion"] = "21"
// BuildTools now resolves 1.21.2 to the 1.21.3 build, so build the last 1.21.2 build.
extra["cadence.integration.buildToolsRevision"] = "4349"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.2-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
