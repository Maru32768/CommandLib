val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "26.1.1"
extra["cadence.integration.javaVersion"] = "25"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.1-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
