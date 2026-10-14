val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "26.1"
extra["commandlib.integration.javaVersion"] = "25"
// BuildTools now resolves 26.1 to the 26.1.2 build, so build the last 26.1 build.
extra["commandlib.integration.buildToolsRevision"] = "4611"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.1-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
