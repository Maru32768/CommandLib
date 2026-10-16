val mcProtocol by configurations.creating

// A smoke target for Folia: the paper artifact with the Folia fixture in integration-test/shared/folia, not the Paper suite.
extra["commandlib.integration.platform"] = "folia"
extra["commandlib.integration.minecraftVersion"] = "26.3"
extra["commandlib.integration.javaVersion"] = "25"
extra["commandlib.integration.botJavaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.3-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
