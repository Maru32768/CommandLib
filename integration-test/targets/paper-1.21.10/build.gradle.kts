val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "1.21.10"
extra["cadence.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.9-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
