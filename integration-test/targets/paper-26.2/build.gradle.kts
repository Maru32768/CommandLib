val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "26.2"
extra["cadence.integration.javaVersion"] = "25"
extra["cadence.integration.botJavaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.2")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
