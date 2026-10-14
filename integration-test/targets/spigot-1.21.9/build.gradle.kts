val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.21.9"
extra["commandlib.integration.javaVersion"] = "21"
// BuildTools now resolves 1.21.9 to the 1.21.10 build, so build the last 1.21.9 build.
extra["commandlib.integration.buildToolsRevision"] = "4540"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.9-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
