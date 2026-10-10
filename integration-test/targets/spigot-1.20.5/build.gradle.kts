val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.20.5"
extra["commandlib.integration.javaVersion"] = "21"
// BuildTools now resolves 1.20.5 to the build of the following hotfix release, so build the last 1.20.5 build.
extra["commandlib.integration.buildToolsRevision"] = "4134"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.20.6-2-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
