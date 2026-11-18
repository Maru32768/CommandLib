val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.20.6"
extra["cadence.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.20.6-2-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
